package de.vantrex.skysens.client.features.witch;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.HudRenderFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.core.hud.MovableHud;
import de.vantrex.skysens.client.features.witch.config.YearOfTheWitchAccordion;
import de.vantrex.skysens.client.skyblock.event.SackItemsAddedEvent;
import de.vantrex.skysens.client.skyblock.event.SackItemsRemovedEvent;
import de.vantrex.skysens.client.skyblock.time.SkyblockCalendar;
import de.vantrex.skysens.client.util.ChatUtil;
import de.vantrex.skysens.client.util.ItemUtil;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Feeds the Witch's Sack menu and the sack events to {@link WitchIngredientService} and draws the ingredients of the
 * current profile in a HUD. Only reads chat and menus, it never clicks or sends anything.
 */
@SkySensFeature
public class WitchIngredientFeature implements AfterTickFeature, HudRenderFeature {

    /** The sack is read twice a second while it is open, so its amounts are always the latest. */
    private static final int MENU_INTERVAL_TICKS = 10;

    private final WitchIngredientHud hud = new WitchIngredientHud();
    private final List<MovableHud> movableHuds = List.of(new Movable());
    private int ticks;

    public WitchIngredientFeature() {
        final WitchIngredientService service = WitchIngredientService.getInstance();
        EventListenerRegistry.REGISTRY.registerListener(event -> {
            if (this.isActive()) {
                service.onAdded(event.getItems(), event.getWindowMs());
            }
        }, SackItemsAddedEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(event -> {
            if (this.isActive()) {
                service.onRemoved(event.getItems(), event.getWindowMs());
            }
        }, SackItemsRemovedEvent.class);
    }

    @Override
    public void afterTick() {
        // the sack is read outside the event too, so the HUD is right once the next one starts
        final WitchIngredientService service = WitchIngredientService.getInstance();
        if (++this.ticks % MENU_INTERVAL_TICKS == 0
                && Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen
                && screen.getTitle().getString().trim().equals(WitchIngredientService.MENU_TITLE)) {
            service.onSackMenu(menuItems(screen.getMenu()));
        }
        if (showsHud()) {
            this.hud.update(service.current(), config());
        }
    }

    @Override
    public void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (showsHud()) {
            this.hud.draw(graphics, config());
        }
    }

    @Override
    public List<MovableHud> movableHuds() {
        return this.movableHuds;
    }

    private static List<WitchIngredientService.MenuItem> menuItems(AbstractContainerMenu menu) {
        final List<WitchIngredientService.MenuItem> items = new ArrayList<>();
        // the last 36 slots are the player's inventory
        final int containerSlots = Math.max(0, menu.slots.size() - 36);
        for (int i = 0; i < containerSlots; i++) {
            final ItemStack stack = menu.slots.get(i).getItem();
            final List<String> lore = ItemUtil.strippedLore(stack);
            if (!lore.isEmpty()) {
                items.add(new WitchIngredientService.MenuItem(ChatUtil.strip(stack.getHoverName().getString()), lore));
            }
        }
        return items;
    }

    private static YearOfTheWitchAccordion config() {
        return SkysensConfig.getInstance().miscCategory.yearOfTheWitch;
    }

    /** Counts all year round, so the amounts are right when the next Year of the Witch starts. */
    @Override
    public boolean isActive() {
        return config().enabled && SkysensClient.getInstance().isOnSkyBlock();
    }

    /** Whether the HUD is shown: during a Year of the Witch unless that option is off. */
    private static boolean showsHud() {
        return !config().onlyDuringEvent || SkyblockCalendar.isYearOfTheWitch();
    }

    /** The table in the HUD editor, stored in the existing position and scale options. */
    private final class Movable implements MovableHud {

        @Override
        public String name() {
            return "Year of the Witch";
        }

        @Override
        public boolean isEnabled() {
            return config().enabled;
        }

        /** {@link #afterTick} doesn't build the lines while the HUD is hidden, e.g. off SkyBlock. */
        private WitchIngredientHud hud() {
            if (WitchIngredientFeature.this.hud.isEmpty()) {
                WitchIngredientFeature.this.hud.update(WitchIngredientService.getInstance().current(), config());
            }
            return WitchIngredientFeature.this.hud;
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
            final YearOfTheWitchAccordion defaults = new YearOfTheWitchAccordion();
            config().x = defaults.x;
            config().y = defaults.y;
            config().scale = defaults.scale;
        }

    }

}
