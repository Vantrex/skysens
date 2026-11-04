package de.vantrex.skysens.client.features.lootshare;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.event.SkysensListener;
import de.vantrex.skysens.client.core.feature.Feature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.skyblock.profit.ProfitService;
import de.vantrex.skysens.client.skyblock.event.SackItemsAddedEvent;
import de.vantrex.skysens.client.skyblock.sack.SackItem;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalDouble;

/**
 * Counts items that went straight into sacks for the profit tracker, from the {@link SackItemsAddedEvent} of
 * {@code SackTracker}. Removed items are withdrawals and ignored.
 */
@SkySensFeature
public class SackProfitFeature implements Feature, SkysensListener<SackItemsAddedEvent> {

    /** How long a rare drop is remembered, so its sack entry isn't counted a second time. */
    private static final long DROP_MEMORY_MILLIS = 60_000L;
    private static final List<RecentDrop> RECENT_DROPS = new ArrayList<>();

    /** Amount of a drop already counted by the loot share feature, not yet matched with a sack entry. */
    private static final class RecentDrop {
        private final String name;
        private final long time;
        private int remaining;

        private RecentDrop(String name, int remaining, long time) {
            this.name = name;
            this.remaining = remaining;
            this.time = time;
        }
    }

    /** Called by the loot share feature for every rare drop it counted, a matching sack entry is skipped later. */
    static synchronized void rememberDrop(String name, int amount) {
        RECENT_DROPS.add(new RecentDrop(name.toLowerCase(), amount, System.currentTimeMillis()));
    }

    /** Removes up to {@code amount} of the item from the remembered drops and returns what is left over. */
    private static synchronized int subtractRememberedDrops(String name, int amount) {
        final long now = System.currentTimeMillis();
        final String key = name.toLowerCase();
        int left = amount;
        for (final Iterator<RecentDrop> it = RECENT_DROPS.iterator(); it.hasNext(); ) {
            final RecentDrop drop = it.next();
            if (now - drop.time > DROP_MEMORY_MILLIS) {
                it.remove();
            } else if (left > 0 && drop.name.equals(key)) {
                final int used = Math.min(left, drop.remaining);
                drop.remaining -= used;
                left -= used;
                if (drop.remaining <= 0) {
                    it.remove();
                }
            }
        }
        return left;
    }

    public SackProfitFeature() {
        EventListenerRegistry.REGISTRY.registerListener(this, SackItemsAddedEvent.class);
    }

    @Override
    public void handle(SackItemsAddedEvent event) {
        if (!this.isActive()) {
            return;
        }
        double total = 0;
        boolean any = false;
        for (final SackItem entry : event.getItems()) {
            final int uncounted = subtractRememberedDrops(entry.name(), entry.amount());
            if (uncounted <= 0) {
                continue;
            }
            final OptionalDouble value = LootShareFeature.dropValue(uncounted + "x " + entry.name());
            if (value.isPresent()) {
                total += value.getAsDouble();
                any = true;
            }
        }
        if (any) {
            // one event per message, not per item
            ProfitService.getInstance().record(ProfitService.detectActivity(), total);
        }
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().generalCategory.profitTracker.enabled;
    }

}
