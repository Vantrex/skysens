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
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.ChatFormatting;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

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
    /** Vanilla draws the action bar around 68 pixels above the bottom, the progress goes one line above it. */
    private static final int ACTION_BAR_OFFSET = 68 + 16;

    private int ticks;

    public SlayerRngMeterFeature() {
        final SlayerRngMeterService service = SlayerRngMeterService.getInstance();
        EventListenerRegistry.REGISTRY.registerListener(service::onQuestEnd, SlayerQuestEndEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(event -> service.onBoss(event.getBoss()), SlayerBossSpawnEvent.class);
        HudElementRegistry.attachElementAfter(VanillaHudElements.OVERLAY_MESSAGE,
                Identifier.fromNamespaceAndPath("skysens", "slayer_rng_meter"), this::extractProgress);
    }

    /** Draws the progress as its own line above the action bar, Hypixel's action bar stays as it is. */
    private void extractProgress(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (!this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final Component progress = SlayerRngMeterService.getInstance().getActionBar();
        if (progress == null) {
            return;
        }
        final Minecraft client = Minecraft.getInstance();
        final Font font = client.font;
        final int x = (client.getWindow().getGuiScaledWidth() - font.width(progress)) / 2;
        final int y = client.getWindow().getGuiScaledHeight() - ACTION_BAR_OFFSET;
        graphics.text(font, progress, x, y, 0xFFFFFFFF, true);
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
            final ItemLore lore = stack.isEmpty() ? null : stack.get(DataComponents.LORE);
            if (lore == null) {
                continue;
            }
            final List<String> lines = new ArrayList<>();
            for (final Component line : lore.lines()) {
                final String text = ChatFormatting.stripFormatting(line.getString());
                if (text != null) {
                    lines.add(text.trim());
                }
            }
            final String name = ChatFormatting.stripFormatting(stack.getHoverName().getString());
            items.add(new SlayerRngMeterService.MenuItem(name == null ? "" : name.trim(), lines));
        }
        return items;
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().slayerCategory.enabled;
    }

}
