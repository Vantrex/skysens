package de.vantrex.skysens.client.features.slayer;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.features.slayer.rng.SlayerRngMeterService;
import de.vantrex.skysens.client.skyblock.event.SlayerBossCocoonEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerBossSpawnEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerQuestEndEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerQuestStartEvent;
import de.vantrex.skysens.client.skyblock.slayer.SlayerTracker;
import de.vantrex.skysens.client.util.ScoreboardUtil;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/** Feeds slayer related chat lines, {@link SlayerTracker}'s quest events, player input and the sidebar to {@link SlayerService}. */
@SkySensFeature
public class SlayerTrackingFeature implements GameMessageListeningFeature, AfterTickFeature {

    /** The sidebar is read once a second, quest progress doesn't need more. */
    private static final int SIDEBAR_INTERVAL_TICKS = 20;
    /** Vanilla draws the action bar around 68 pixels above the bottom, the estimate goes one line above it. */
    private static final int ACTION_BAR_OFFSET = 68 + 16;
    /** Moves the estimate up a line while the RNG meter progress takes the line right above the action bar. */
    private static final int LINE_HEIGHT = 12;

    private float lastYRot = Float.NaN;
    private float lastXRot = Float.NaN;
    private int ticks;

    public SlayerTrackingFeature() {
        final SlayerService service = SlayerService.getInstance();
        EventListenerRegistry.REGISTRY.registerListener(service::onQuestStart, SlayerQuestStartEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(service::onBossSpawn, SlayerBossSpawnEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(service::onBossCocoon, SlayerBossCocoonEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(service::onQuestEnd, SlayerQuestEndEvent.class);
        final SlayerKillsLeftService killsLeftService = SlayerKillsLeftService.getInstance();
        EventListenerRegistry.REGISTRY.registerListener(killsLeftService::onQuestStart, SlayerQuestStartEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(killsLeftService::onBossSpawn, SlayerBossSpawnEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(killsLeftService::onQuestEnd, SlayerQuestEndEvent.class);
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("skysens", "slayer_kills_left"), this::extractKillsLeft);
    }

    /** Draws the kills-left estimate as its own line above the action bar, Hypixel's action bar stays as it is. */
    private void extractKillsLeft(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final Component killsLeft = SlayerKillsLeftService.getInstance().getActionBar();
        if (killsLeft == null) {
            return;
        }
        final Minecraft client = Minecraft.getInstance();
        final Font font = client.font;
        final int offset = SlayerRngMeterService.getInstance().getActionBar() == null
                ? ACTION_BAR_OFFSET
                : ACTION_BAR_OFFSET + LINE_HEIGHT;
        final int x = (client.getWindow().getGuiScaledWidth() - font.width(killsLeft)) / 2;
        final int y = client.getWindow().getGuiScaledHeight() - offset;
        graphics.text(font, killsLeft, x, y, 0xFFFFFFFF, true);
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final String text = ChatFormatting.stripFormatting(message.getString());
        if (text != null) {
            SlayerService.getInstance().onChatLine(text);
        }
    }

    @Override
    public void afterTick() {
        final Minecraft client = Minecraft.getInstance();
        final LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        final List<String> sidebar = ++this.ticks % SIDEBAR_INTERVAL_TICKS == 0 ? ScoreboardUtil.getSidebarLines() : null;
        SlayerService.getInstance().onTick(this.hadInput(client.options, player), sidebar);
        SlayerKillsLeftService.getInstance().onTick(sidebar);
    }

    /**
     * Whether the player looked around, swung or held a movement, attack or use key. Position alone isn't used:
     * mobs knock an AFK player around.
     */
    private boolean hadInput(Options options, LocalPlayer player) {
        final float yRot = player.getYRot();
        final float xRot = player.getXRot();
        final boolean looked = yRot != this.lastYRot || xRot != this.lastXRot;
        this.lastYRot = yRot;
        this.lastXRot = xRot;
        return looked || player.swinging || anyDown(options.keyUp, options.keyDown, options.keyLeft, options.keyRight,
                options.keyJump, options.keyAttack, options.keyUse);
    }

    private static boolean anyDown(KeyMapping... keys) {
        for (final KeyMapping key : keys) {
            if (key.isDown()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().slayerCategory.enabled;
    }

}
