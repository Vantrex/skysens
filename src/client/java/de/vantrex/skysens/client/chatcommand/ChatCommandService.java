package de.vantrex.skysens.client.chatcommand;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.chatcommand.impl.BuffsCommand;
import de.vantrex.skysens.client.chatcommand.impl.CoinFlipCommand;
import de.vantrex.skysens.client.chatcommand.impl.CoordsCommand;
import de.vantrex.skysens.client.chatcommand.impl.DiceCommand;
import de.vantrex.skysens.client.chatcommand.impl.FpsCommand;
import de.vantrex.skysens.client.chatcommand.impl.HelpCommand;
import de.vantrex.skysens.client.chatcommand.impl.PingCommand;
import de.vantrex.skysens.client.chatcommand.impl.ServerCommand;
import de.vantrex.skysens.client.chatcommand.impl.TpsCommand;
import de.vantrex.skysens.client.chatcommand.impl.WarpToCommand;
import de.vantrex.skysens.client.chatcommand.impl.ZoneCommand;
import de.vantrex.skysens.client.chatcommand.integration.ChatCommandBridge;
import de.vantrex.skysens.client.chatcommand.integration.OdinChatCommandsBridge;
import de.vantrex.skysens.client.chatcommand.integration.SkyHanniPartyCommandsBridge;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.chat.ChatCommandsCategory;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;

/** Parses chat channel messages, runs matching {@link ChatCommand}s and sends their replies. */
public final class ChatCommandService {

    public static final String PREFIX = "!";

    /** Short delay so the reply appears after the command in chat, like a human answer. */
    private static final long REPLY_DELAY_MS = 250;
    private static final long MIN_SEND_GAP_MS = 1000;
    private static final int MAX_CHAT_LENGTH = 256;

    @Getter
    private static final ChatCommandService instance = new ChatCommandService();

    @Getter
    private final ChatCommandRegistry registry = new ChatCommandRegistry();
    private final List<ChatCommandBridge> bridges = List.of(new OdinChatCommandsBridge(), new SkyHanniPartyCommandsBridge());
    private long lastAnsweredAt;
    private long nextSendAt;

    private ChatCommandService() {
        this.registry.register(new HelpCommand(this.registry));
        this.registry.register(new PingCommand());
        this.registry.register(new TpsCommand());
        this.registry.register(new ZoneCommand());
        this.registry.register(new ServerCommand());
        this.registry.register(new BuffsCommand());
        this.registry.register(new WarpToCommand());
        this.registry.register(new CoordsCommand());
        this.registry.register(new FpsCommand());
        this.registry.register(new CoinFlipCommand());
        this.registry.register(new DiceCommand());
    }

    /** Handles a received chat message. Called on the client thread. */
    public void onChatMessage(@NotNull String rawMessage) {
        final String message = ChatFormatting.stripFormatting(rawMessage);
        if (message == null) {
            return;
        }
        for (final ChatChannel channel : ChatChannel.values()) {
            final Matcher matcher = channel.getPattern().matcher(message);
            if (matcher.matches()) {
                this.handle(channel, matcher.group(1), matcher.group(2).trim());
                return;
            }
        }
    }

    private void handle(ChatChannel channel, String sender, String text) {
        if (!text.startsWith(PREFIX) || !this.isChannelEnabled(channel)) {
            return;
        }
        final ChatCommandsCategory config = SkysensConfig.getInstance().chatCommandsCategory;
        if (config.onlyOwnMessages && !this.isOwnName(sender)) {
            return;
        }
        final List<String> parts = Arrays.asList(text.substring(PREFIX.length()).trim().split("\\s+"));
        final String label = parts.getFirst().toLowerCase(Locale.ROOT);
        if (label.isEmpty()) {
            return;
        }
        final Optional<ChatCommand> command = this.registry.find(label)
                .filter(ChatCommand::isEnabled)
                .filter(found -> found.channels().contains(channel));
        if (command.isEmpty()) {
            return;
        }
        if (config.deferToOtherMods) {
            for (final ChatCommandBridge bridge : this.bridges) {
                if (bridge.handles(channel, label, sender)) {
                    ClientUtil.sendDebug(bridge.name() + " answers !" + label + " in " + channel.getPrefix() + " chat, skipping");
                    return;
                }
            }
        }
        if (command.get().usesCooldown()) {
            final long now = System.currentTimeMillis();
            if (now - this.lastAnsweredAt < (long) (config.cooldownSeconds * 1000)) {
                ClientUtil.sendDebug("Chat command !" + label + " from " + sender + " is on cooldown");
                return;
            }
            this.lastAnsweredAt = now;
        }

        final ChatCommandContext context = new ChatCommandContext(channel, sender, label,
                List.copyOf(parts.subList(1, parts.size())), reply -> this.send(channel, reply));
        try {
            command.get().execute(context);
        } catch (RuntimeException e) {
            ClientUtil.sendDebug("Chat command !" + label + " failed: " + e);
        }
    }

    private boolean isChannelEnabled(ChatChannel channel) {
        return switch (channel) {
            case COOP -> SkysensConfig.getInstance().chatCommandsCategory.coopEnabled;
            case PARTY -> SkysensConfig.getInstance().chatCommandsCategory.partyEnabled;
        };
    }

    private boolean isOwnName(String sender) {
        final var player = Minecraft.getInstance().player;
        return player != null && player.getName().getString().equalsIgnoreCase(sender);
    }

    /**
     * Sends a message to a chat channel. Messages are spaced at least {@value #MIN_SEND_GAP_MS} ms apart,
     * so a burst (e.g. several drops at once) doesn't get us muted. Safe to call from any thread.
     */
    public void send(@NotNull ChatChannel channel, @NotNull String message) {
        final String command = channel.getReplyCommand() + " " + sanitize(message);
        final String trimmed = command.length() > MAX_CHAT_LENGTH ? command.substring(0, MAX_CHAT_LENGTH) : command;
        final long delay;
        synchronized (this) {
            final long now = System.currentTimeMillis();
            final long sendAt = Math.max(now + REPLY_DELAY_MS, this.nextSendAt);
            this.nextSendAt = sendAt + MIN_SEND_GAP_MS;
            delay = sendAt - now;
        }
        SkysensClient.SCHEDULER.schedule(() -> Minecraft.getInstance().execute(() -> {
            final ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.sendCommand(trimmed);
            }
        }), delay, TimeUnit.MILLISECONDS);
    }

    /** Removes formatting and line breaks, and makes sure a reply can never trigger a command itself. */
    private static String sanitize(String reply) {
        String clean = ChatFormatting.stripFormatting(reply);
        clean = clean == null ? "" : clean.replaceAll("[\\r\\n§]", " ").trim();
        while (clean.startsWith(PREFIX) || clean.startsWith("/")) {
            clean = clean.substring(1).trim();
        }
        return clean;
    }

}
