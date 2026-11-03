package de.vantrex.skysens.client.features.fishing.trophyfrog;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.core.feature.HudRenderFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
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
    /** The profile name is read from the tab list once a second. */
    private static final int PROFILE_INTERVAL_TICKS = 20;

    private final TrophyFrogHud hud = new TrophyFrogHud();
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
        this.ticks++;
        if (this.ticks % PROFILE_INTERVAL_TICKS == 0) {
            service.refreshProfile();
        }
        if (this.ticks % MENU_INTERVAL_TICKS == 0
                && Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen
                && screen.getTitle().getString().trim().equals(TrophyFrogService.MENU_TITLE)) {
            service.refreshProfile();
            service.onMenu(menuItems(screen.getMenu()));
        }
        this.hud.update(service.current(), config());
    }

    @Override
    public void renderHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        final TrophyFrogAccordion config = config();
        if (config.onlyOnLotusAtoll
                && LocationService.getInstance().getCurrentLocation() != SkyblockLocationEnum.LOTUS_ATOLL) {
            return;
        }
        this.hud.draw(graphics, config);
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

    @Override
    public boolean isActive() {
        return config().enabled;
    }

}
