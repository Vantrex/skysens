package de.vantrex.skysens.client.features.slayer.rng;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.skyblock.event.SlayerBossSpawnEvent;
import de.vantrex.skysens.client.skyblock.event.SlayerQuestEndEvent;
import de.vantrex.skysens.client.skyblock.mayor.MayorService;
import de.vantrex.skysens.client.skyblock.slayer.SlayerTracker;
import de.vantrex.skysens.client.util.ChatUtil;
import de.vantrex.skysens.client.util.ItemUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * Feeds the completion message, the slayer boss and the open RNG meter menu to {@link SlayerRngMeterService}. Only
 * reads chat and menus, it never clicks or sends anything.
 */
@SkySensFeature
public class SlayerRngMeterFeature implements GameMessageListeningFeature, AfterTickFeature {

    /** The RNG meter menu is read twice a second while it is open, so a drop (de)selected in it is noticed. */
    private static final int MENU_INTERVAL_TICKS = 10;

    private int ticks;

    public SlayerRngMeterFeature() {
        final SlayerRngMeterService service = SlayerRngMeterService.getInstance();
        EventListenerRegistry.REGISTRY.registerListener(service::onQuestEnd, SlayerQuestEndEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(event -> service.onBoss(event.getBoss()), SlayerBossSpawnEvent.class);
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final String text = ChatFormatting.stripFormatting(message.getString());
        if (text != null) {
            SlayerRngMeterService.getInstance().onChatLine(ChatUtil.normalize(text));
        }
    }

    @Override
    public void afterTick() {
        if (!this.isActive()) {
            return;
        }
        MayorService.getInstance().refreshIfNeeded();
        final SlayerRngMeterService service = SlayerRngMeterService.getInstance();
        service.onBoss(SlayerTracker.getInstance().getBoss());
        if (++this.ticks % MENU_INTERVAL_TICKS == 0
                && Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> screen) {
            final String title = screen.getTitle().getString();
            if (title.endsWith(" RNG Meter")) {
                service.onMenu(title, menuItems(screen.getMenu()));
            }
        }
    }

    private static List<SlayerRngMeterService.MenuItem> menuItems(AbstractContainerMenu menu) {
        final List<SlayerRngMeterService.MenuItem> items = new ArrayList<>();
        // the last 36 slots are the player's inventory
        final int containerSlots = Math.max(0, menu.slots.size() - 36);
        for (int i = 0; i < containerSlots; i++) {
            final ItemStack stack = menu.slots.get(i).getItem();
            final List<String> lore = ItemUtil.strippedLore(stack);
            if (!lore.isEmpty()) {
                items.add(new SlayerRngMeterService.MenuItem(ChatUtil.strip(stack.getHoverName().getString()), lore));
            }
        }
        return items;
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().slayerCategory.enabled;
    }

}
