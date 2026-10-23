package de.vantrex.skysens.client.features.bazaar;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.features.bazaar.BazaarOrderRepository.Order;
import de.vantrex.skysens.client.skyblock.player.PlayerService;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks the user's open bazaar orders for the order alerts: from the "Your Bazaar Orders" GUI (exact prices)
 * and from the [Bazaar] chat messages (placed, filled, claimed, cancelled).
 */
@SkySensFeature
public class BazaarOrderTrackingFeature implements GameMessageListeningFeature, AfterTickFeature {

    // "[Bazaar] Buy Order Setup! 2x Triple-Strike V for 48,580,945 coins."
    private static final Pattern SETUP = Pattern.compile("^\\[Bazaar] (Buy Order|Sell Offer) Setup! ([\\d,]+)x (.+) for ([\\d,.]+) coins\\.$");
    // "[Bazaar] Your Sell Offer for 7x Apex Dragon Shard was filled!"
    private static final Pattern FILLED = Pattern.compile("^\\[Bazaar] Your (Buy Order|Sell Offer) for ([\\d,]+)x (.+) was filled!$");
    // "[Bazaar] Claimed 745,446 coins from selling 1x Soul Eater I at 753,928 each!"
    private static final Pattern CLAIM_SELL = Pattern.compile("^\\[Bazaar] Claimed [\\d,.]+ coins from selling ([\\d,]+)x (.+) at [\\d,.]+ each!$");
    // "[Bazaar] Claimed 2x Gravity VI worth 7,961,009 coins bought for 3,980,505 each!"
    private static final Pattern CLAIM_BUY = Pattern.compile("^\\[Bazaar] Claimed ([\\d,]+)x (.+) worth [\\d,.]+ coins bought for [\\d,.]+ each!$");
    // "[Bazaar] Cancelled! Refunded 1x Duplex I from cancelling Sell Offer!"
    private static final Pattern CANCEL_SELL = Pattern.compile("^\\[Bazaar] Cancelled! Refunded [\\d,]+x (.+) from cancelling Sell Offer!$");
    // "[Bazaar] Cancelled! Refunded 1,234 coins from cancelling Buy Order!"
    private static final Pattern CANCEL_BUY = Pattern.compile("^\\[Bazaar] Cancelled! Refunded ([\\d,.]+) coins from cancelling Buy Order!$");
    /** Hypixel appends a repeat counter to identical messages: "... (2)". */
    private static final Pattern COUNTER = Pattern.compile("\\s*\\(\\d+\\)$");

    private static final Pattern GUI_TITLE = Pattern.compile(".*Bazaar Orders");
    private static final Pattern AMOUNT_LINE = Pattern.compile("^(?:Order|Offer) amount: ([\\d,.]+[kKmM]?)x?$");
    private static final Pattern PRICE_LINE = Pattern.compile("^Price per unit: ([\\d,.]+) coins$");
    private static final Pattern FILLED_LINE = Pattern.compile("^Filled: ([\\d,.]+[kKmM]?)\\s*/\\s*([\\d,.]+[kKmM]?)");
    private static final Pattern PERCENT = Pattern.compile("(\\d+(?:\\.\\d+)?)%");

    private final PlayerService playerService = PlayerService.getInstance();
    private List<Order> lastScan = List.of();

    public BazaarOrderTrackingFeature() {
        BazaarOrderService.getInstance(); // starts the order book polling
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive()) {
            return;
        }
        String text = ChatFormatting.stripFormatting(message.getString());
        if (text == null) {
            return;
        }
        text = COUNTER.matcher(text.trim()).replaceFirst("");
        if (!text.startsWith("[Bazaar]")) {
            return;
        }
        final BazaarOrderService service = BazaarOrderService.getInstance();
        Matcher m = SETUP.matcher(text);
        if (m.matches()) {
            service.orderPlaced(isBuy(m.group(1)), number(m.group(2)), m.group(3), decimal(m.group(4)));
            return;
        }
        m = FILLED.matcher(text);
        if (m.matches()) {
            final boolean buy = isBuy(m.group(1));
            service.orderFilled(buy, number(m.group(2)), m.group(3));
            service.notifyFilled(buy, number(m.group(2)), m.group(3));
            return;
        }
        m = CLAIM_SELL.matcher(text);
        if (m.matches()) {
            service.orderClaimed(false, number(m.group(1)), m.group(2));
            return;
        }
        m = CLAIM_BUY.matcher(text);
        if (m.matches()) {
            service.orderClaimed(true, number(m.group(1)), m.group(2));
            return;
        }
        m = CANCEL_SELL.matcher(text);
        if (m.matches()) {
            service.sellOfferCancelled(m.group(1));
            return;
        }
        m = CANCEL_BUY.matcher(text);
        if (m.matches()) {
            service.buyOrderCancelled(decimal(m.group(1)));
        }
    }

    /** Reads the orders from the "Your Bazaar Orders" GUI while it is open, and syncs when they changed. */
    @Override
    public void afterTick() {
        BazaarOrderService.getInstance().deliverPendingAlerts();
        final Minecraft client = Minecraft.getInstance();
        if (!(client.gui.screen() instanceof AbstractContainerScreen<?> screen)
                || !GUI_TITLE.matcher(screen.getTitle().getString()).matches()
                || this.playerService.getCurrentPlayerName() == null) {
            this.lastScan = List.of();
            return;
        }
        final AbstractContainerMenu menu = screen.getMenu();
        // the last 36 slots are the player's inventory
        final int containerSlots = Math.max(0, menu.slots.size() - 36);
        boolean loaded = false;
        final List<Order> orders = new ArrayList<>();
        for (int i = 0; i < containerSlots; i++) {
            final ItemStack stack = menu.slots.get(i).getItem();
            if (stack.isEmpty()) {
                continue;
            }
            loaded = true;
            final Order order = this.parseOrder(stack);
            if (order != null) {
                orders.add(order);
            }
        }
        if (!loaded || this.sameAs(orders)) {
            return;
        }
        this.lastScan = orders;
        BazaarOrderService.getInstance().syncFromGui(copyOf(orders));
    }

    private Order parseOrder(ItemStack stack) {
        final String name = stack.getHoverName().getString();
        final boolean buy = name.startsWith("BUY ");
        if (!buy && !name.startsWith("SELL ")) {
            return null;
        }
        final ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return null;
        }
        boolean own = false;
        long amount = 0;
        long filled = -1;
        double price = 0;
        double percent = -1;
        for (final Component line : lore.lines()) {
            final String text = line.getString().trim();
            if (text.startsWith("By:") && text.endsWith(this.playerService.getCurrentPlayerName())) {
                own = true;
            }
            Matcher m = AMOUNT_LINE.matcher(text);
            if (m.matches()) {
                amount = (long) abbreviated(m.group(1));
            }
            m = PRICE_LINE.matcher(text);
            if (m.matches()) {
                price = decimal(m.group(1));
            }
            m = FILLED_LINE.matcher(text);
            if (m.find()) {
                filled = (long) abbreviated(m.group(1));
                if (amount <= 0) {
                    amount = (long) abbreviated(m.group(2));
                }
            } else if (text.startsWith("Filled:")) {
                final Matcher p = PERCENT.matcher(text);
                if (p.find()) {
                    percent = Double.parseDouble(p.group(1));
                }
            }
        }
        if (!own || amount <= 0 || price <= 0) {
            return null;
        }
        if (filled < 0) {
            filled = percent >= 0 ? Math.round(amount * percent / 100) : 0;
        }
        return new Order(name.substring(name.indexOf(' ') + 1).trim(), buy, price, amount, filled);
    }

    private boolean sameAs(List<Order> orders) {
        if (orders.size() != this.lastScan.size()) {
            return false;
        }
        for (int i = 0; i < orders.size(); i++) {
            final Order a = orders.get(i);
            final Order b = this.lastScan.get(i);
            if (a.buy != b.buy || !a.item.equals(b.item) || a.pricePerUnit != b.pricePerUnit
                    || a.amount != b.amount || a.filled != b.filled) {
                return false;
            }
        }
        return true;
    }

    private static List<Order> copyOf(List<Order> orders) {
        return orders.stream().map(Order::copy).toList();
    }

    private static boolean isBuy(String type) {
        return type.startsWith("Buy");
    }

    private static long number(String text) {
        return Long.parseLong(text.replace(",", ""));
    }

    private static double decimal(String text) {
        return Double.parseDouble(text.replace(",", ""));
    }

    /** "1,234", "1.5k", "2M". */
    private static double abbreviated(String text) {
        final String clean = text.replace(",", "");
        final char last = Character.toLowerCase(clean.charAt(clean.length() - 1));
        if (last == 'k') {
            return Double.parseDouble(clean.substring(0, clean.length() - 1)) * 1_000;
        }
        if (last == 'm') {
            return Double.parseDouble(clean.substring(0, clean.length() - 1)) * 1_000_000;
        }
        return Double.parseDouble(clean);
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().bazaarCategory.orderAlerts.enabled;
    }

}
