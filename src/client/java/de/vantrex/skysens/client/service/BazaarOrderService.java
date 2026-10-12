package de.vantrex.skysens.client.service;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.bazaar.OrderAlerts;
import de.vantrex.skysens.client.feature.afk.AfkTracker;
import de.vantrex.skysens.client.feature.combat.CombatTracker;
import de.vantrex.skysens.client.model.Notification;
import de.vantrex.skysens.client.model.NotificationSound;
import de.vantrex.skysens.client.price.BazaarOrderBook;
import de.vantrex.skysens.client.repository.BazaarOrderRepository.Order;
import de.vantrex.skysens.client.repository.RepositoryRegistry;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.ExponentialBackoff;
import de.vantrex.skysens.client.util.NumberUtil;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Keeps track of the user's open bazaar orders and alerts when they are outbid, undercut or filled.
 * The order book is fetched on SkySens' scheduler thread, one request per interval for all products.
 */
public class BazaarOrderService {

    @Getter
    private static final BazaarOrderService instance = new BazaarOrderService();

    private static final Logger LOGGER = LoggerFactory.getLogger("skysens");
    /** Chat shows the sell offer total after tax (1.125%); used to estimate the unit price until the GUI is read. */
    private static final double SELL_TAX = 0.01125;
    private static final double EPSILON = 0.0001;

    private final Object lock = new Object();
    /** Failed fetches wait 30s, then 1m, 2m, ... up to 30m, so an API outage isn't hammered. */
    private final ExponentialBackoff backoff = new ExponentialBackoff(30_000, 30 * 60_000);
    private long nextPollAt;
    /** Outbid/undercut alerts held back during combat or runs, the latest one per order. Guarded by {@link #lock}. */
    private final Map<Order, Runnable> pendingPriceAlerts = new IdentityHashMap<>();

    private BazaarOrderService() {
        // the interval is read on every check, so config changes apply without restarting anything
        SkysensClient.SCHEDULER.scheduleWithFixedDelay(this::pollSafely, 10, 5, TimeUnit.SECONDS);
    }

    // ---- tracking ----

    /** A "Buy Order/Sell Offer Setup!" message; {@code total} is the coins from the message. */
    public void orderPlaced(boolean buy, long amount, String item, double total) {
        if (amount <= 0) {
            return;
        }
        final double unit = buy ? total / amount : Math.round(total / amount / (1 - SELL_TAX) * 10) / 10.0;
        final Order order = new Order(item, buy, unit, amount, 0);
        order.estimated = !buy;
        synchronized (this.lock) {
            this.orders().add(order);
        }
    }

    /** "Your ... was filled!": the order only waits to be claimed now. */
    public void orderFilled(boolean buy, long amount, String item) {
        synchronized (this.lock) {
            Order match = null;
            for (final Order order : this.orders()) {
                if (order.buy == buy && order.item.equalsIgnoreCase(item) && !order.complete
                        && (match == null || order.amount == amount && match.amount != amount)) {
                    match = order;
                }
            }
            if (match == null) {
                // not tracked yet (placed before SkySens saw it); track it so claiming works
                return;
            }
            match.complete = true;
            match.filled = match.amount;
        }
    }

    /** Claimed items or coins of an order; removes it once everything is claimed. */
    public void orderClaimed(boolean buy, long amount, String item) {
        synchronized (this.lock) {
            final Order match = this.find(buy, item, true);
            if (match == null) {
                return;
            }
            match.amount -= amount;
            match.filled = Math.max(0, match.filled - amount);
            if (match.amount <= 0) {
                this.orders().remove(match);
            }
        }
    }

    /** A cancelled sell offer names its item. */
    public void sellOfferCancelled(String item) {
        synchronized (this.lock) {
            final Order match = this.find(false, item, false);
            if (match != null) {
                this.orders().remove(match);
            }
        }
    }

    /** A cancelled buy order only names the refunded coins, so the order with the closest remaining value goes. */
    public void buyOrderCancelled(double refunded) {
        synchronized (this.lock) {
            Order best = null;
            double bestDiff = Double.MAX_VALUE;
            for (final Order order : this.orders()) {
                if (!order.buy) {
                    continue;
                }
                final double diff = Math.abs((order.amount - order.filled) * order.pricePerUnit - refunded);
                if (diff < bestDiff) {
                    best = order;
                    bestDiff = diff;
                }
            }
            if (best != null && bestDiff <= Math.max(2, refunded * 0.02)) {
                this.orders().remove(best);
            }
        }
    }

    /** Replaces the orders with what the "Your Bazaar Orders" GUI shows, which is exact. */
    public void syncFromGui(List<Order> scanned) {
        synchronized (this.lock) {
            for (final Order order : scanned) {
                final Order known = this.orders().stream()
                        .filter(o -> o.buy == order.buy && o.item.equalsIgnoreCase(order.item)
                                && Math.abs(o.pricePerUnit - order.pricePerUnit) < EPSILON)
                        .findFirst().orElse(null);
                if (known != null) {
                    order.lastAlertedPrice = known.lastAlertedPrice;
                    order.complete = known.complete || order.filled >= order.amount;
                } else {
                    order.complete = order.filled >= order.amount;
                }
            }
            this.orders().clear();
            this.orders().addAll(scanned);
        }
    }

    private Order find(boolean buy, String item, boolean preferComplete) {
        Order match = null;
        for (final Order order : this.orders()) {
            if (order.buy != buy || !order.item.equalsIgnoreCase(item)) {
                continue;
            }
            if (match == null || preferComplete && order.complete && !match.complete) {
                match = order;
            }
        }
        return match;
    }

    private List<Order> orders() {
        return RepositoryRegistry.BAZAAR_ORDER_REPOSITORY.orders;
    }

    // ---- alerts ----

    /** "Ready to claim" alert, from the filled chat message. Runs on the client thread. */
    public void notifyFilled(boolean buy, long amount, String item) {
        final OrderAlerts config = this.config();
        if (!config.enabled || !config.filledAlerts) {
            return;
        }
        final String type = buy ? "Buy Order" : "Sell Offer";
        final String detail = amount + "x " + item;
        this.alert(Component.literal(type + " filled!").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD),
                Component.literal(detail + " - ready to claim").withStyle(ChatFormatting.YELLOW),
                Component.literal("[Bazaar] " + type + " filled: " + detail + " - ready to claim").withStyle(ChatFormatting.GREEN),
                true);
    }

    private void pollSafely() {
        if (!this.backoff.canAttempt()) {
            return;
        }
        try {
            this.poll();
        } catch (Exception e) {
            final long delay = this.backoff.onFailure();
            LOGGER.warn("Could not check bazaar orders ({} failed in a row), retrying in {}s",
                    this.backoff.failures(), delay / 1000, e);
        }
    }

    private void poll() throws Exception {
        final OrderAlerts config = this.config();
        if (!config.enabled || !(config.outbidAlerts || config.undercutAlerts)) {
            return;
        }
        if (config.onlyOnSkyBlock && !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        if (System.currentTimeMillis() < this.nextPollAt) {
            return;
        }
        synchronized (this.lock) {
            if (this.orders().stream().noneMatch(o -> !o.complete && (o.buy ? config.outbidAlerts : config.undercutAlerts))) {
                return;
            }
        }
        this.nextPollAt = System.currentTimeMillis() + (long) (Math.max(20f, config.pollIntervalSeconds) * 1000);
        final BazaarOrderBook book = BazaarOrderBook.fetch();
        this.backoff.onSuccess();

        // shown by deliverPendingAlerts on the client thread, once out of combat and runs and not AFK
        synchronized (this.lock) {
            for (final Order order : this.orders()) {
                if (order.complete) {
                    continue;
                }
                final BazaarOrderBook.Prices prices = book.find(order.item);
                if (prices == null) {
                    continue;
                }
                final Runnable alert = order.buy ? this.checkOutbid(config, order, prices) : this.checkUndercut(config, order, prices);
                if (alert != null) {
                    this.pendingPriceAlerts.put(order, alert);
                }
            }
        }
    }

    private @Nullable Runnable checkOutbid(OrderAlerts config, Order order, BazaarOrderBook.Prices prices) {
        if (!config.outbidAlerts) {
            return null;
        }
        final double best = prices.highestBuyOrder();
        if (best <= order.pricePerUnit + EPSILON) {
            order.lastAlertedPrice = 0;
            this.pendingPriceAlerts.remove(order);
            return null;
        }
        return this.alertFor(config, order, best, best - order.pricePerUnit, "Outbid", "top buy order", "+");
    }

    private @Nullable Runnable checkUndercut(OrderAlerts config, Order order, BazaarOrderBook.Prices prices) {
        if (!config.undercutAlerts) {
            return null;
        }
        final double best = prices.lowestSellOffer();
        if (best <= 0 || best >= order.pricePerUnit - EPSILON) {
            order.lastAlertedPrice = 0;
            this.pendingPriceAlerts.remove(order);
            return null;
        }
        return this.alertFor(config, order, best, order.pricePerUnit - best, "Undercut", "lowest sell offer", "-");
    }

    private @Nullable Runnable alertFor(OrderAlerts config, Order order, double best, double diff, String title, String what, String sign) {
        if (!this.shouldAlert(config, order, diff)) {
            return null;
        }
        order.lastAlertedPrice = best;
        return this.priceAlert(order, best, diff, title, what, sign);
    }

    /** Alerts once when outbid/undercut, then only when the better price moved a lot further away from yours. */
    private boolean shouldAlert(OrderAlerts config, Order order, double diff) {
        if (diff < config.minimumCoins || percentOf(order, diff) < config.minimumPercent) {
            return false;
        }
        if (order.lastAlertedPrice <= 0) {
            return true;
        }
        final double lastDiff = Math.abs(order.pricePerUnit - order.lastAlertedPrice);
        return percentOf(order, diff - lastDiff) >= config.realertPercent;
    }

    private Runnable priceAlert(Order order, double best, double diff, String title, String what, String sign) {
        final String detail = String.format(Locale.ROOT, "%s - %s %s (%s%s, %.1f%%) | yours %s",
                order.item, what, price(best), sign, price(diff), percentOf(order, diff), price(order.pricePerUnit));
        return () -> this.alert(
                Component.literal(title + "!").withStyle(ChatFormatting.RED, ChatFormatting.BOLD),
                Component.literal(detail).withStyle(ChatFormatting.YELLOW),
                Component.literal("[Bazaar] " + title + ": " + detail).withStyle(ChatFormatting.RED),
                false);
    }

    /** {@code coins} as percent of the order's price. */
    private static double percentOf(Order order, double coins) {
        return coins / order.pricePerUnit * 100;
    }

    /** Short price: "1.2M", "12.5k", and one decimal below 1k since many bazaar items cost only a few coins. */
    private static String price(double value) {
        if (value >= 999.5) {
            return NumberUtil.formatCoins(value);
        }
        return java.math.BigDecimal.valueOf(value).setScale(1, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    /**
     * Shows the held back outbid/undercut alerts once the player is out of combat, not AFK and not in a dungeon or Kuudra run
     * (each condition configurable). Alerts of orders that were filled or removed meanwhile are dropped. Client thread.
     */
    public void deliverPendingAlerts() {
        final OrderAlerts config = this.config();
        final List<Runnable> alerts;
        synchronized (this.lock) {
            if (this.pendingPriceAlerts.isEmpty() || !canShowPriceAlerts(config)) {
                return;
            }
            alerts = new ArrayList<>();
            this.pendingPriceAlerts.forEach((order, alert) -> {
                if (!order.complete && this.orders().contains(order)) {
                    alerts.add(alert);
                }
            });
            this.pendingPriceAlerts.clear();
        }
        alerts.forEach(Runnable::run);
    }

    private static boolean canShowPriceAlerts(OrderAlerts config) {
        if (config.waitOutOfInstances && LocationService.getInstance().isInInstance()) {
            return false;
        }
        if (SkysensConfig.getInstance().generalCategory.holdNotificationsWhileAfk && AfkTracker.getInstance().isAfk()) {
            return false;
        }
        return !config.waitOutOfCombat || CombatTracker.getInstance().isOutOfCombatFor(config.outOfCombatSeconds);
    }

    /** Client thread. {@code chatLine} is used where only chat can show it (type CHAT, or off SkyBlock). */
    private void alert(Component title, Component subtitle, Component chatLine, boolean filled) {
        final OrderAlerts config = this.config();
        final NotificationSound sound = !config.playSound ? null : filled
                ? new NotificationSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.5F)
                : new NotificationSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 0.8F);
        if (!SkysensClient.getInstance().isOnSkyBlock()) {
            if (sound != null) {
                ClientUtil.playSound(sound.sound(), sound.volume(), sound.pitch());
            }
            ClientUtil.sendMessage(chatLine);
            return;
        }
        final boolean chat = config.displayType == de.vantrex.skysens.client.enums.NotificationDisplayTypeEnum.CHAT;
        NotificationService.getInstance().sendNotification(Notification.builder()
                .text(chat ? chatLine : title)
                .subtitle(chat ? null : subtitle)
                .displayType(config.displayType)
                .displayTime(Duration.ofSeconds(4))
                .sound(sound)
                .build());
    }

    private OrderAlerts config() {
        return SkysensConfig.getInstance().bazaarCategory.orderAlerts;
    }

}
