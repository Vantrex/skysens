package de.vantrex.skysens.client.features.fishing.trophyfrog;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.core.feature.HudRenderFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.core.hud.MovableHud;
import de.vantrex.skysens.client.features.fishing.config.TrophyFrogAccordion;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.util.ChatUtil;
import de.vantrex.skysens.client.util.ItemUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Feeds the Trophy Frogs menu and the catch messages to {@link TrophyFrogService} and draws the counts of the current
 * profile in a HUD. Only reads chat and menus, it never clicks or sends anything.
 */
@SkySensFeature
public class TrophyFrogFeature implements GameMessageListeningFeature, AfterTickFeature, HudRenderFeature {

    /** The menu is read twice a second while it is open. */
    private static final int MENU_INTERVAL_TICKS = 10;

    private final TrophyFrogHud hud = new TrophyFrogHud();
    private final List<MovableHud> movableHuds = List.of(new Movable());
    private int ticks;

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        TrophyFrogService.getInstance().onChatLine(ChatUtil.normalize(ChatUtil.strip(message.getString())));
    }

    @Override
    public void afterTick() {
        if (!this.isActive()) {
            return;
        }
        final TrophyFrogService service = TrophyFrogService.getInstance();
        if (++this.ticks % MENU_INTERVAL_TICKS == 0
                && Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen
                && screen.getTitle().getString().trim().equals(TrophyFrogService.MENU_TITLE)) {
            service.onMenu(menuItems(screen.getMenu()));
        }
        service.onLotusAtoll();
        this.hud.update(service.current(), config());
    }

    @Override
    public void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        this.hud.draw(graphics, config());
    }

    @Override
    public List<MovableHud> movableHuds() {
        return this.movableHuds;
    }

    private static List<TrophyFrogService.MenuItem> menuItems(AbstractContainerMenu menu) {
        final List<TrophyFrogService.MenuItem> items = new ArrayList<>();
        // the last 36 slots are the player's inventory
        final int containerSlots = Math.max(0, menu.slots.size() - 36);
        for (int i = 0; i < containerSlots; i++) {
            final ItemStack stack = menu.slots.get(i).getItem();
            final List<String> lore = ItemUtil.strippedLore(stack);
            if (!lore.isEmpty()) {
                items.add(new TrophyFrogService.MenuItem(ChatUtil.strip(stack.getHoverName().getString()), lore));
            }
        }
        return items;
    }

    private static TrophyFrogAccordion config() {
        return SkysensConfig.getInstance().fishingCategory.trophyFrogs;
    }

    /**
     * Only on the Lotus Atoll: frogs are only caught there and the Trophy Frogs menu is at Researcher Ribery there, so
     * elsewhere there is nothing to read or show.
     */
    @Override
    public boolean isActive() {
        return config().enabled
                && LocationService.getInstance().getCurrentLocation() == SkyblockLocationEnum.LOTUS_ATOLL;
    }

    /** The table in the HUD editor, stored in the existing position and scale options. */
    private final class Movable implements MovableHud {

        @Override
        public String name() {
            return "Trophy Frogs";
        }

        @Override
        public boolean isEnabled() {
            return config().enabled;
        }

        /** {@link #afterTick} doesn't build the lines while the feature is inactive, e.g. off the Lotus Atoll. */
        private TrophyFrogHud hud() {
            if (TrophyFrogFeature.this.hud.isEmpty()) {
                TrophyFrogFeature.this.hud.update(TrophyFrogService.getInstance().current(), config());
            }
            return TrophyFrogFeature.this.hud;
        }

        @Override
        public int width() {
            return this.hud().width();
        }

        @Override
        public int height() {
            return this.hud().height();
        }

        @Override
        public float scale() {
            return config().scale;
        }

        @Override
        public void setScale(float scale) {
            config().scale = scale;
        }

        @Override
        public float left(int screenWidth, int screenHeight) {
            return screenWidth * config().x / 100f;
        }

        @Override
        public float top(int screenWidth, int screenHeight) {
            return screenHeight * config().y / 100f;
        }

        @Override
        public void moveTo(float left, float top, int screenWidth, int screenHeight) {
            config().x = Math.clamp(left * 100f / screenWidth, 0f, 100f);
            config().y = Math.clamp(top * 100f / screenHeight, 0f, 100f);
        }

        @Override
        public void renderPreview(GuiGraphicsExtractor graphics) {
            this.hud().drawContent(graphics);
        }

        @Override
        public void reset() {
            final TrophyFrogAccordion defaults = new TrophyFrogAccordion();
            config().x = defaults.x;
            config().y = defaults.y;
            config().scale = defaults.scale;
        }

    }

}
