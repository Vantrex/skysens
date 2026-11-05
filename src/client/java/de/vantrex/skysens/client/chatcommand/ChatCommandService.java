package de.vantrex.skysens.client.chatcommand;

import de.vantrex.skysens.client.features.activity.ActivityCommand;
import de.vantrex.skysens.client.features.dungeon.RunsCommand;
import de.vantrex.skysens.client.features.slayer.SlayerCommand;
import de.vantrex.skysens.client.features.volcano.VolcanoCommand;
import de.vantrex.skysens.client.features.volcano.WarpMeCommand;
import de.vantrex.skysens.client.features.warp.WarpToCommand;
import de.vantrex.skysens.client.skyblock.profit.ProfitCommand;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.chatcommand.impl.*;
import de.vantrex.skysens.client.integration.odin.OdinChatCommandsBridge;
import de.vantrex.skysens.client.integration.skyhanni.SkyHanniPartyCommandsBridge;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.chatcommand.config.ChatCommandsCategory;
import de.vantrex.skysens.client.util.Brand;
import de.vantrex.skysens.client.util.ChatUtil;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses chat channel messages, runs matching {@link ChatCommand}s and sends their replies. */
public final class ChatCommandService {

    public static final String PREFIX = "!";

    /** Short delay so the reply appears after the command in chat, like a human answer. */
    private static final long REPLY_DELAY_MS = 250;
    private static final long MIN_SEND_GAP_MS = 1000;
    private static final int MAX_CHAT_LENGTH = 256;
    /** How long a sent message counts as recent for the anti-spam suffix. */
    private static final long REPEAT_WINDOW_MS = 60_000;
    /** What two messages may differ in and still count as the same: numbers and spaces. */
    private static final Pattern REPEAT_IGNORED = Pattern.compile("[\\d\\s]+");

    @Getter
    private static final ChatCommandService instance = new ChatCommandService();

    @Getter
    private final ChatCommandRegistry registry = new ChatCommandRegistry();
    private final List<ChatCommandBridge> bridges = List.of(new OdinChatCommandsBridge(), new SkyHanniPartyCommandsBridge());
    private long lastAnsweredAt;
    private long nextSendAt;
    /** Recently sent messages (see {@link #isRepeat(String, long)}) with the time they were sent (ms). */
    private final Map<String, Long> recentMessages = new HashMap<>();

    private ChatCommandService() {
        this.registry.register(new HelpCommand(this.registry));
        this.registry.register(new PingCommand());
        this.registry.register(new TpsCommand());
        this.registry.register(new ProfitCommand());
        this.registry.register(new ZoneCommand());
        this.registry.register(new ServerCommand());
        this.registry.register(new BuffsCommand());
        this.registry.register(new WarpToCommand());
        this.registry.register(new CoordsCommand());
        this.registry.register(new FpsCommand());
        this.registry.register(new CoinFlipCommand());
        this.registry.register(new DiceCommand());
        this.registry.register(new ActivityCommand());
        this.registry.register(new RunsCommand());
        this.registry.register(new SlayerCommand());
        this.registry.register(new UptimeCommand());
        this.registry.register(new VolcanoCommand());
        this.registry.register(new WarpMeCommand());
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
        final Optional<ChatCommand> command = this.registry.find(label);
        if (command.isEmpty()) {
            return;
        }
        if (!command.get().isEnabled()) {
            ClientUtil.sendDebug("Ignoring !" + label + " from " + sender + ", the command is disabled in the config");
            return;
        }
        if (!command.get().channels().contains(channel)) {
            ClientUtil.sendDebug("Ignoring !" + label + " from " + sender + ", it can't be used in " + channel.getPrefix() + " chat");
            return;
        }
        if (config.deferToOtherMods && command.get().defersToOtherMods()) {
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
                List.copyOf(parts.subList(1, parts.size())),
                channel == ChatChannel.PRIVATE
                        ? reply -> this.sendPrivateMessage(sender, reply)
                        : reply -> this.send(channel, reply));
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
            case PRIVATE -> SkysensConfig.getInstance().chatCommandsCategory.privateEnabled;
        };
    }

    private boolean isOwnName(String sender) {
        final var player = Minecraft.getInstance().player;
        return player != null && player.getName().getString().equalsIgnoreCase(sender);
    }

    /**
     * Sends a message to a chat channel. Messages are spaced at least {@value #MIN_SEND_GAP_MS} ms apart,
     * so a burst (e.g. several drops at once) doesn't get us muted. Safe to call from any thread.
     * Not for {@link ChatChannel#PRIVATE}, which needs a receiver: use {@link #sendPrivateMessage(String, String)}.
     */
    public void send(@NotNull ChatChannel channel, @NotNull String message) {
        if (channel == ChatChannel.PRIVATE) {
            throw new IllegalArgumentException("Private messages need a receiver, use sendPrivateMessage");
        }
        this.sendCommand(channel.getReplyCommand() + " ", Brand.uncoloredPrefix() + sanitize(message));
    }

    /**
     * Sends a private message ({@code /msg}) to a player, spaced like {@link #send(ChatChannel, String)}.
     * Invalid player names are ignored. Safe to call from any thread.
     */
    public void sendPrivateMessage(@NotNull String playerName, @NotNull String message) {
        if (!playerName.matches("\\w{1,16}")) {
            return;
        }
        this.sendCommand("msg " + playerName + " ", Brand.uncoloredPrefix() + sanitize(message));
    }

    /**
     * Sends the command, e.g. {@code "pc "} followed by the message. A message that is (nearly) the same as one sent in
     * the last {@value #REPEAT_WINDOW_MS} ms, in any channel or to any player, gets a random anti-spam suffix (see
     * {@link ChatUtil#withAntiSpam(String)}), so Hypixel doesn't block it as "the same message twice". Other messages
     * are sent as they are. The message is shortened first, so the suffix always fits.
     */
    private void sendCommand(@NotNull String command, @NotNull String message) {
        final int maxLength = MAX_CHAT_LENGTH - command.length() - ChatUtil.ANTI_SPAM_SUFFIX_LENGTH;
        final String shortened = message.length() > maxLength ? message.substring(0, Math.max(0, maxLength)) : message;

        final String full;
        final long delay;
        synchronized (this) {
            final long now = System.currentTimeMillis();
            full = command + (this.isRepeat(shortened, now) ? ChatUtil.withAntiSpam(shortened) : shortened);
            final long sendAt = Math.max(now + REPLY_DELAY_MS, this.nextSendAt);
            this.nextSendAt = sendAt + MIN_SEND_GAP_MS;
            delay = sendAt - now;
        }
        SkysensClient.SCHEDULER.schedule(() -> Minecraft.getInstance().execute(() -> {
            final ClientPacketListener connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.sendCommand(full);
            }
        }), delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Whether the message is (nearly) the same as one sent in the last {@value #REPEAT_WINDOW_MS} ms, and remembers
     * it. Messages count as the same when they only differ in case, numbers or spaces, e.g. "you're #2 in line" and
     * "you're #3 in line". Call while synchronized on this.
     */
    private boolean isRepeat(String message, long now) {
        this.recentMessages.values().removeIf(sentAt -> now - sentAt > REPEAT_WINDOW_MS);
        final String key = REPEAT_IGNORED.matcher(message.toLowerCase(Locale.ROOT)).replaceAll("");
        return this.recentMessages.put(key, now) != null;
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
