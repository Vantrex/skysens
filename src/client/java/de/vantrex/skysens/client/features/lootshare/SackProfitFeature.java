package de.vantrex.skysens.client.features.lootshare;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.skyblock.profit.ProfitService;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Counts items that went straight into sacks for the profit tracker. Hypixel summarizes them every few seconds as
 * "[Sacks] +179 items. (Last 12s.)", the hover of "+179 items" lists every item. Removed items ("-5 items") are
 * withdrawals and ignored.
 */
@SkySensFeature
public class SackProfitFeature implements GameMessageListeningFeature {

    private static final Pattern SACKS_PATTERN = Pattern.compile("^\\[Sacks]\\s.*");
    /** The "+179 items" part of the message, whose hover lists the added items. */
    private static final Pattern ADDED_PATTERN = Pattern.compile("^\\+[\\d,]+ items?\\b.*");
    /** "+27 Gabagool Distillate (Combat Sack)", also tolerates a missing sack name. */
    private static final Pattern LINE_PATTERN = Pattern.compile("^\\s*\\+([\\d,]+)x? (.+?)(?:\\s+\\([^()]*\\))?\\s*$");

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

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final String text = ChatFormatting.stripFormatting(message.getString());
        if (text == null || !SACKS_PATTERN.matcher(text.trim()).matches()) {
            return;
        }
        final Component hover = findAddedHover(message, null);
        if (hover == null) {
            return;
        }
        final String hoverText = ChatFormatting.stripFormatting(hover.getString());
        if (hoverText == null) {
            return;
        }
        double total = 0;
        boolean any = false;
        for (final String line : hoverText.split("\\R")) {
            final Matcher matcher = LINE_PATTERN.matcher(line);
            if (!matcher.matches()) {
                continue;
            }
            final int amount = Integer.parseInt(matcher.group(1).replace(",", ""));
            final String name = matcher.group(2).trim();
            final int uncounted = subtractRememberedDrops(name, amount);
            if (uncounted <= 0) {
                continue;
            }
            final OptionalDouble value = LootShareFeature.dropValue(uncounted + "x " + name);
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

    /** The hover text of the "+N items" component, searching the siblings and inheriting the parent's hover. */
    private static @Nullable Component findAddedHover(Component component, @Nullable Component inherited) {
        Component hover = inherited;
        if (component.getStyle().getHoverEvent() instanceof HoverEvent.ShowText showText) {
            hover = showText.value();
        }
        final String own = ChatFormatting.stripFormatting(component.getString());
        if (hover != null && own != null && ADDED_PATTERN.matcher(own.trim()).matches()) {
            return hover;
        }
        for (final Component sibling : component.getSiblings()) {
            final Component found = findAddedHover(sibling, hover);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().generalCategory.profitTracker.enabled;
    }

}
