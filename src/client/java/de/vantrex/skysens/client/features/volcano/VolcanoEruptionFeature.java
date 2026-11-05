package de.vantrex.skysens.client.features.volcano;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.chatcommand.ChatChannel;
import de.vantrex.skysens.client.chatcommand.ChatCommandService;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.features.volcano.config.VolcanoEruptionAccordion;
import de.vantrex.skysens.client.notification.Notification;
import de.vantrex.skysens.client.notification.NotificationDisplayTypeEnum;
import de.vantrex.skysens.client.notification.NotificationService;
import de.vantrex.skysens.client.notification.NotificationSound;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.util.ClientUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Shows a title when the Blazing Volcano on the Crimson Isle starts erupting. The state is read from the
 * "Volcano: CHILL" line in the tab list, every 10 seconds, and every second after joining a server until the tab list
 * has it. An eruption that is already going on when joining is shown too, and can be posted in party/co-op chat.
 * <p>
 * Also shows a "Same Server" title when joining a Crimson Isle server again shortly after leaving it, which happens a
 * lot while warping around to find an eruption.
 */
@SkySensFeature
public class VolcanoEruptionFeature implements AfterTickFeature {

    private static final int CHECK_INTERVAL_TICKS = 200;
    /** Right after joining, until the tab list has the volcano line. */
    private static final int JOIN_CHECK_INTERVAL_TICKS = 20;
    /** After this long on a server without a volcano line, the tab list widget is probably turned off. */
    private static final int MISSING_LINE_HINT_TICKS = 600;
    private static final NotificationSound CATACLYSMIC_SOUND =
            new NotificationSound(SoundEvents.ENDER_DRAGON_GROWL, 1.0F, 1.0F);
    private static final NotificationSound SAME_SERVER_SOUND =
            new NotificationSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.7F);

    private int ticks;
    /** Ticks on {@link #server} without finding the volcano line. */
    private int ticksWithoutLine;
    /** The missing line hint is sent once per game session. */
    private boolean missingLineHintSent;
    /** The server the state below was read on, so a new Crimson Isle server counts as joining. */
    private @Nullable String server;
    /** The last state read on {@link #server}, null until it was read once. */
    private @Nullable VolcanoExplosivity last;
    private @Nullable String lastUnknown;
    /** Crimson Isle servers we left, with the time we left them (ms). */
    private final Map<String, Long> leftServers = new HashMap<>();
    /** The last eruption posted in chat, so rejoining its server doesn't post it again. */
    private @Nullable String lastAnnouncedServer;
    private @Nullable VolcanoExplosivity lastAnnounced;

    @Override
    public void afterTick() {
        if (LocationService.getInstance().getCurrentLocation() != SkyblockLocationEnum.CRIMSON_ISLE) {
            this.leaveServer();
            return;
        }
        final String currentServer = SkysensClient.getInstance().getServerName();
        if (!Objects.equals(currentServer, this.server)) {
            this.leaveServer();
            this.joinServer(currentServer);
        }
        if (!config().enabled) {
            return;
        }
        if (++this.ticks < (this.last == null ? JOIN_CHECK_INTERVAL_TICKS : CHECK_INTERVAL_TICKS)) {
            return;
        }
        this.checkMissingLine(this.ticks);
        this.ticks = 0;
        final VolcanoExplosivity current = this.readTabList();
        if (current == null || current == this.last) {
            return;
        }
        this.last = current;
        this.ticksWithoutLine = 0;
        if (!current.isErupting()) {
            VolcanoFoundEruption.clear();
        } else if (!config().onlyCataclysmic || current == VolcanoExplosivity.CATACLYSMIC) {
            if (this.server != null) {
                VolcanoFoundEruption.set(this.server, current);
            }
            this.notify(current);
            this.announce(current);
        }
    }

    private void joinServer(@Nullable String newServer) {
        this.server = newServer;
        this.last = null;
        this.ticks = 0;
        this.ticksWithoutLine = 0;
        if (newServer == null || !config().sameServerAlert) {
            return;
        }
        final long now = System.currentTimeMillis();
        final Long leftAt = this.leftServers.get(newServer);
        if (leftAt != null && now - leftAt <= config().sameServerSeconds * 1000L) {
            this.notifySameServer(newServer, (now - leftAt) / 1000L);
        }
    }

    /** Remembers when we left the current server, for the same server alert. */
    private void leaveServer() {
        if (this.server != null) {
            final long now = System.currentTimeMillis();
            final long maxAgeMs = config().sameServerSeconds * 1000L;
            this.leftServers.values().removeIf(leftAt -> now - leftAt > maxAgeMs);
            this.leftServers.put(this.server, now);
            VolcanoFoundEruption.clear();
        }
        this.server = null;
        this.last = null;
    }

    /**
     * While the tab list doesn't have the volcano line yet, this is called every second. After 30 seconds on a server
     * the widget is most likely turned off: tell the player once, and from then on only check every 10 seconds.
     */
    private void checkMissingLine(int elapsedTicks) {
        if (this.last != null) {
            return;
        }
        this.ticksWithoutLine += elapsedTicks;
        if (this.ticksWithoutLine < MISSING_LINE_HINT_TICKS) {
            return;
        }
        // pretend a state was read so the slower interval applies; the next real read still notifies
        this.last = VolcanoExplosivity.INACTIVE;
        if (!this.missingLineHintSent) {
            this.missingLineHintSent = true;
            ClientUtil.sendPrefixed("§eYour tab list has no §6Volcano §eline, so the volcano eruption title can't work. "
                    + "Turn on the Volcano widget in your SkyBlock tab list widget settings.");
        }
    }

    private @Nullable VolcanoExplosivity readTabList() {
        final String text = VolcanoTabList.read();
        if (text == null) {
            return null;
        }
        final VolcanoExplosivity explosivity = VolcanoExplosivity.fromTabList(text);
        if (explosivity == null && !text.equals(this.lastUnknown)) {
            // a level we don't know yet, report it once so it can be added
            this.lastUnknown = text;
            ClientUtil.sendDebug("Unknown volcano state in the tab list: " + text);
        }
        return explosivity;
    }

    private void notify(VolcanoExplosivity explosivity) {
        NotificationService.getInstance().sendNotification(Notification.builder()
                .text(Component.literal("Volcano Eruption!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .subtitle(Component.literal(explosivity.name()).withStyle(explosivity.getColor()))
                .displayType(NotificationDisplayTypeEnum.TITLE)
                .displayTime(Duration.ofSeconds(3))
                .sound(explosivity == VolcanoExplosivity.CATACLYSMIC ? CATACLYSMIC_SOUND : null)
                .level(config().level)
                .build());
    }

    private void announce(VolcanoExplosivity explosivity) {
        final List<String> players = messagePlayers();
        if ((!config().announce && players.isEmpty()) || this.server == null
                || (this.server.equals(this.lastAnnouncedServer) && explosivity == this.lastAnnounced)) {
            return;
        }
        this.lastAnnouncedServer = this.server;
        this.lastAnnounced = explosivity;
        final String message = explosivity.name() + " volcano eruption on " + this.server;
        final boolean warpMe = explosivity == VolcanoExplosivity.CATACLYSMIC
                && SkysensConfig.getInstance().chatCommandsCategory.commands.warpMe;
        final ChatCommandService chat = ChatCommandService.getInstance();
        if (config().announce) {
            for (final ChatChannel channel : config().announceChannel.getChannels()) {
                chat.send(channel, warpMe ? message + " - type !warpme to get warped" : message);
            }
        }
        for (final String player : players) {
            chat.sendPrivateMessage(player, warpMe ? message + " - reply !warpme to get warped" : message);
        }
    }

    /** The names from "Message Players", without blanks and duplicates. */
    private static List<String> messagePlayers() {
        final String names = config().messagePlayers;
        if (names == null || names.isBlank()) {
            return List.of();
        }
        return Arrays.stream(names.split(","))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();
    }

    private void notifySameServer(String serverName, long secondsAgo) {
        NotificationService.getInstance().sendNotification(Notification.builder()
                .text(Component.literal("Same Server!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD))
                .subtitle(Component.literal(serverName + " - left " + secondsAgo + "s ago").withStyle(ChatFormatting.GRAY))
                .displayType(NotificationDisplayTypeEnum.TITLE)
                .displayTime(Duration.ofSeconds(2))
                .sound(SAME_SERVER_SOUND)
                .level(config().level)
                .build());
    }

    private static VolcanoEruptionAccordion config() {
        return SkysensConfig.getInstance().miscCategory.volcanoEruption;
    }

    @Override
    public boolean isActive() {
        return config().enabled || config().sameServerAlert;
    }

}
