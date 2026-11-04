package de.vantrex.skysens.client.features.volcano;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.features.volcano.config.VolcanoEruptionAccordion;
import de.vantrex.skysens.client.notification.Notification;
import de.vantrex.skysens.client.notification.NotificationDisplayTypeEnum;
import de.vantrex.skysens.client.notification.NotificationService;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.TabListUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.Objects;

/**
 * Shows a title when the Blazing Volcano on the Crimson Isle starts erupting. The state is read from the
 * "Volcano: CHILL" line in the tab list, every 10 seconds, and every second after joining a server until the tab list
 * has it. An eruption that is already going on when joining is shown too.
 */
@SkySensFeature
public class VolcanoEruptionFeature implements AfterTickFeature {

    private static final String TAB_PREFIX = "Volcano:";
    private static final int CHECK_INTERVAL_TICKS = 200;
    /** Right after joining, until the tab list has the volcano line. */
    private static final int JOIN_CHECK_INTERVAL_TICKS = 20;

    private int ticks;
    /** The server the state below was read on, so a new Crimson Isle server counts as joining. */
    private @Nullable String server;
    /** The last state read on {@link #server}, null until it was read once. */
    private @Nullable VolcanoExplosivity last;
    private @Nullable String lastUnknown;

    @Override
    public void afterTick() {
        if (LocationService.getInstance().getCurrentLocation() != SkyblockLocationEnum.CRIMSON_ISLE) {
            this.server = null;
            this.last = null;
            return;
        }
        final String currentServer = SkysensClient.getInstance().getServerName();
        if (!Objects.equals(currentServer, this.server)) {
            this.server = currentServer;
            this.last = null;
            this.ticks = 0;
        }
        if (++this.ticks < (this.last == null ? JOIN_CHECK_INTERVAL_TICKS : CHECK_INTERVAL_TICKS)) {
            return;
        }
        this.ticks = 0;
        final VolcanoExplosivity current = this.readTabList();
        if (current == null || current == this.last) {
            return;
        }
        this.last = current;
        if (current.isErupting() && (!config().onlyCataclysmic || current == VolcanoExplosivity.CATACLYSMIC)) {
            this.notify(current);
        }
    }

    private @Nullable VolcanoExplosivity readTabList() {
        for (final String line : TabListUtil.lines()) {
            if (!line.startsWith(TAB_PREFIX)) {
                continue;
            }
            final String text = line.substring(TAB_PREFIX.length()).trim();
            final VolcanoExplosivity explosivity = VolcanoExplosivity.fromTabList(text);
            if (explosivity == null && !text.equals(this.lastUnknown)) {
                // a level we don't know yet, report it once so it can be added
                this.lastUnknown = text;
                ClientUtil.sendDebug("Unknown volcano state in the tab list: " + text);
            }
            return explosivity;
        }
        return null;
    }

    private void notify(VolcanoExplosivity explosivity) {
        NotificationService.getInstance().sendNotification(Notification.builder()
                .text(Component.literal("Volcano Eruption!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD))
                .subtitle(Component.literal(explosivity.name()).withStyle(explosivity.getColor()))
                .displayType(NotificationDisplayTypeEnum.TITLE)
                .displayTime(Duration.ofSeconds(3))
                .level(config().level)
                .build());
    }

    private static VolcanoEruptionAccordion config() {
        return SkysensConfig.getInstance().miscCategory.volcanoEruption;
    }

    @Override
    public boolean isActive() {
        return config().enabled;
    }

}
