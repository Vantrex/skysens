package de.vantrex.skysens.client.features.slayer.rng;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.slayer.config.SlayerCategory;
import de.vantrex.skysens.client.features.slayer.config.SlayerRngMeterAccordion;
import de.vantrex.skysens.client.notification.Notification;
import de.vantrex.skysens.client.notification.NotificationService;
import de.vantrex.skysens.client.notification.NotificationSound;
import de.vantrex.skysens.client.persistence.RepositoryRegistry;
import de.vantrex.skysens.client.skyblock.event.SlayerQuestEndEvent;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.NumberUtil;
import lombok.Getter;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks the slayer RNG meters for the drop chosen per slayer in the config, whether or not it is selected in /rng.
 * The stored XP comes from the "RNG Meter - N Stored XP" line of the completion message (the slayer is the one of the
 * level line right before it), the XP each drop needs and the drop selected in /rng from the RNG meter menu. Shows the
 * progress in the action bar after every boss and notifies once the meter is full. Client thread.
 */
public final class SlayerRngMeterService {

    private static final Pattern SLAYER_LEVEL = Pattern.compile("^(\\w+) Slayer LVL \\d+ - .*$");
    private static final Pattern RNG_METER_XP = Pattern.compile("^RNG Meter - ([\\d,]+) Stored XP\\b.*$");
    /** Not confirmed in game: the meter as percent of the selected drop. Only used if Hypixel shows it like this. */
    private static final Pattern RNG_METER_PERCENT = Pattern.compile("^RNG Meter - ([\\d.]+)%.*$");
    /** The menu of one slayer's meter, e.g. "Revenant Horror RNG Meter". */
    private static final Pattern MENU_TITLE = Pattern.compile("^(.+) RNG Meter$");
    private static final Pattern MENU_MINIMUM = Pattern.compile("^Minimum: Tier (I{1,3}|IV|V)$");
    private static final Pattern MENU_SLAYER_XP = Pattern.compile("^Slayer XP: ([\\d,.]+[kKmM]?)/([\\d,.]+[kKmM]?)$");
    private static final Pattern MENU_STORED_XP = Pattern.compile("^Stored Slayer XP: ([\\d,.]+[kKmM]?)$");
    private static final Pattern BOSS_TIER = Pattern.compile("\\b(I{1,3}|IV|V)$");

    @Getter
    private static final SlayerRngMeterService instance = new SlayerRngMeterService();

    private @Nullable SlayerTypeEnum lastSlayerType;
    /** The last boss with tier, e.g. "Revenant Horror IV". The tracker forgets it when the quest ends. */
    private @Nullable String lastBoss;
    /** Completed quests since the last stored XP reading; the XP difference is one boss' XP only if this is 1. */
    private int completionsSinceReading;
    private @Nullable Component actionBar;
    private long actionBarUntilMs;

    private SlayerRngMeterService() {
    }

    public void onQuestEnd(@NotNull SlayerQuestEndEvent event) {
        if (event.getReason() == SlayerQuestEndEvent.Reason.COMPLETED) {
            this.completionsSinceReading++;
        }
    }

    public void onBoss(@Nullable String boss) {
        if (boss != null) {
            this.lastBoss = boss;
        }
    }

    /** Handles a chat line (formatting removed and normalized). */
    public void onChatLine(@NotNull String text) {
        final Matcher level = SLAYER_LEVEL.matcher(text);
        if (level.matches()) {
            this.lastSlayerType = SlayerTypeEnum.bySlayerName(level.group(1));
            return;
        }
        if (!text.startsWith("RNG Meter")) {
            return;
        }
        final SlayerTypeEnum type = this.lastSlayerType;
        final Matcher xp = RNG_METER_XP.matcher(text);
        if (type != null && xp.matches()) {
            this.onStoredXp(type, Long.parseLong(xp.group(1).replace(",", "")));
            return;
        }
        final Matcher percent = RNG_METER_PERCENT.matcher(text);
        final SlayerRngDrop selected = type == null ? null : this.selectedDrop(type);
        if (type != null && selected != null && percent.matches()) {
            final double fraction = Double.parseDouble(percent.group(1)) / 100;
            this.onStoredXp(type, Math.round(fraction * this.requiredXp(type, selected)));
            return;
        }
        ClientUtil.sendDebug("RNG meter: unknown line \"" + text + "\" (slayer " + type + ")");
    }

    private void onStoredXp(SlayerTypeEnum type, long storedXp) {
        final SlayerRngRepository.Meter meter = RepositoryRegistry.SLAYER_RNG_REPOSITORY.meter(type);
        final Long previous = meter.storedXp;
        // a lower value means the meter paid out a drop; more than one completion in between isn't one boss' XP
        if (previous != null && storedXp > previous && this.completionsSinceReading == 1) {
            meter.xpPerBoss = storedXp - previous;
        }
        meter.storedXp = storedXp;
        this.completionsSinceReading = 0;

        final SlayerRngDrop drop = type.getTrackedDrop().apply(config());
        if (drop.isNone()) {
            return;
        }
        final SlayerRngMeterAccordion settings = config().rngMeter;
        if (settings.actionBar) {
            this.actionBar = this.progress(type, meter, drop);
            this.actionBarUntilMs = System.currentTimeMillis() + (long) (settings.actionBarSeconds * 1000);
        }
        this.checkFull(type, meter, drop);
    }

    /** Keeps the progress in the action bar, Hypixel overwrites it with its own bar every second. */
    public void onTick() {
        if (this.actionBar == null) {
            return;
        }
        if (System.currentTimeMillis() > this.actionBarUntilMs) {
            this.actionBar = null;
            return;
        }
        ClientUtil.sendActionBar(this.actionBar);
    }

    /**
     * E.g. "Scythe Blade: 120.5k/489.9k XP (24.6%) | ~74 bosses left | selected in /rng". The needed XP is marked
     * with "~" while it is only the wiki's estimate.
     */
    private Component progress(SlayerTypeEnum type, SlayerRngRepository.Meter meter, SlayerRngDrop drop) {
        final long stored = meter.storedXp == null ? 0 : meter.storedXp;
        final long required = this.requiredXp(type, drop);
        final String estimate = this.learnedXp(type, drop) == null ? "~" : "";
        final MutableComponent out = Component.literal(drop.getItemName() + ": ").withStyle(ChatFormatting.LIGHT_PURPLE)
                .append(Component.literal(formatXp(stored) + "/" + estimate + formatXp(required) + " XP").withStyle(ChatFormatting.WHITE))
                .append(Component.literal(String.format(Locale.ROOT, " (%.1f%%)", Math.min(100.0, stored * 100.0 / Math.max(1, required))))
                        .withStyle(ChatFormatting.GRAY));
        out.append(separator()).append(this.bossesLeft(meter, drop, stored, required));
        out.append(separator()).append(this.selection(type, meter, drop));
        return out;
    }

    private Component bossesLeft(SlayerRngRepository.Meter meter, SlayerRngDrop drop, long stored, long required) {
        final int tier = this.lastTier();
        if (tier > 0 && tier < drop.getMinTier()) {
            return Component.literal("needs Tier " + SlayerRngDrop.tier(drop.getMinTier()) + "+").withStyle(ChatFormatting.RED);
        }
        if (stored >= required) {
            return Component.literal("FULL").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        }
        if (tier > 0 && tier < 3) {
            return Component.literal("Tier III+ fills the meter").withStyle(ChatFormatting.RED);
        }
        if (meter.xpPerBoss == null || meter.xpPerBoss <= 0) {
            return Component.literal("? bosses left").withStyle(ChatFormatting.YELLOW);
        }
        final long left = (required - stored + meter.xpPerBoss - 1) / meter.xpPerBoss;
        return Component.literal("~" + left + (left == 1 ? " boss" : " bosses") + " left").withStyle(ChatFormatting.YELLOW);
    }

    private Component selection(SlayerTypeEnum type, SlayerRngRepository.Meter meter, SlayerRngDrop drop) {
        if (meter.selectedDrop == null) {
            return Component.literal("open /rng to sync").withStyle(ChatFormatting.GRAY);
        }
        return drop == this.selectedDrop(type)
                ? Component.literal("selected in /rng").withStyle(ChatFormatting.GREEN)
                : Component.literal("not selected in /rng").withStyle(ChatFormatting.RED);
    }

    /**
     * Notifies once per fill when the meter has the XP for the tracked drop and the last boss' tier can drop it. Only
     * with the XP read from the RNG meter menu: the wiki's estimates are a few percent off.
     */
    private void checkFull(SlayerTypeEnum type, SlayerRngRepository.Meter meter, SlayerRngDrop drop) {
        final String key = ((Enum<?>) drop).name();
        final long stored = meter.storedXp == null ? 0 : meter.storedXp;
        final Long required = this.learnedXp(type, drop);
        if (required == null) {
            return;
        }
        if (stored < required) {
            meter.notifiedFullDrop = null;
            return;
        }
        final int tier = this.lastTier();
        if (!config().rngMeter.notifyWhenFull || key.equals(meter.notifiedFullDrop) || (tier > 0 && tier < drop.getMinTier())) {
            return;
        }
        meter.notifiedFullDrop = key;
        final String subtitle;
        if (meter.selectedDrop == null) {
            subtitle = drop.getItemName() + " - make sure it is selected in /rng";
        } else if (drop == this.selectedDrop(type)) {
            subtitle = drop.getItemName() + " drops from your next " + type.getBossName() + "!";
        } else {
            subtitle = drop.getItemName() + " - select it in /rng to get it from your next boss";
        }
        NotificationService.getInstance().sendNotification(Notification.builder()
                .text(Component.literal("RNG Meter full!").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD))
                .subtitle(Component.literal(subtitle).withStyle(ChatFormatting.GOLD))
                .displayTime(Duration.ofSeconds(4))
                .sound(new NotificationSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 2.0F))
                .level(SkysensConfig.getInstance().generalCategory.notificationLevels.slayerRngMeterFull)
                .build());
    }

    /**
     * Reads one slayer's RNG meter menu: the XP each drop needs ("Slayer XP: 0/3,093"), the stored XP and which drop
     * is selected. A drop counts as selected when its lore mentions "selected" or "deselect" (not "Click to select!").
     *
     * @param title the menu title
     * @param items name and lore lines (formatting removed) of the menu's items
     */
    public void onMenu(@NotNull String title, @NotNull List<MenuItem> items) {
        final Matcher titleMatcher = MENU_TITLE.matcher(title);
        final SlayerTypeEnum type = titleMatcher.matches() ? SlayerTypeEnum.byBoss(titleMatcher.group(1)) : null;
        if (type == null) {
            return;
        }
        final SlayerRngRepository.Meter meter = RepositoryRegistry.SLAYER_RNG_REPOSITORY.meter(type);
        boolean anyDrop = false;
        String selected = "";
        for (final MenuItem item : items) {
            int minTier = 0;
            Long required = null;
            boolean isSelected = false;
            for (final String line : item.lore()) {
                final Matcher stored = MENU_STORED_XP.matcher(line);
                if (stored.matches()) {
                    meter.storedXp = parseXp(stored.group(1));
                }
                final Matcher minimum = MENU_MINIMUM.matcher(line);
                if (minimum.matches()) {
                    minTier = List.of(SlayerRngDrop.TIERS).indexOf(minimum.group(1));
                }
                final Matcher xp = MENU_SLAYER_XP.matcher(line);
                if (xp.matches()) {
                    required = parseXp(xp.group(2));
                }
                final String lower = line.toLowerCase(Locale.ROOT);
                isSelected |= lower.contains("selected") || lower.contains("deselect");
            }
            final SlayerRngDrop drop = required == null ? null : type.dropByName(item.name(), minTier);
            if (drop == null) {
                continue;
            }
            anyDrop = true;
            meter.requiredXp.put(((Enum<?>) drop).name(), required);
            if (isSelected) {
                selected = ((Enum<?>) drop).name();
            }
        }
        // only a loaded menu with its drops tells that nothing is selected
        if (anyDrop) {
            meter.selectedDrop = selected;
        }
    }

    public record MenuItem(@NotNull String name, @NotNull List<String> lore) {
    }

    private @Nullable SlayerRngDrop selectedDrop(SlayerTypeEnum type) {
        return type.dropByKey(RepositoryRegistry.SLAYER_RNG_REPOSITORY.meter(type).selectedDrop);
    }

    /** The XP the drop needs as read from the RNG meter menu, else the wiki's estimate. */
    private long requiredXp(SlayerTypeEnum type, SlayerRngDrop drop) {
        final Long learned = this.learnedXp(type, drop);
        return learned != null ? learned : drop.getWikiRequiredXp();
    }

    private @Nullable Long learnedXp(SlayerTypeEnum type, SlayerRngDrop drop) {
        return RepositoryRegistry.SLAYER_RNG_REPOSITORY.meter(type).requiredXp.get(((Enum<?>) drop).name());
    }

    /** The tier of the last boss, 0 if unknown. */
    private int lastTier() {
        if (this.lastBoss == null) {
            return 0;
        }
        final Matcher tier = BOSS_TIER.matcher(this.lastBoss.trim());
        return tier.find() ? List.of(SlayerRngDrop.TIERS).indexOf(tier.group(1)) : 0;
    }

    /** "3,093", "12.5k" or "3.5M" as a number. */
    private static long parseXp(String text) {
        final String plain = text.replace(",", "");
        final char unit = Character.toLowerCase(plain.charAt(plain.length() - 1));
        final double factor = unit == 'k' ? 1_000 : unit == 'm' ? 1_000_000 : 1;
        final String number = factor == 1 ? plain : plain.substring(0, plain.length() - 1);
        return Math.round(Double.parseDouble(number) * factor);
    }

    private static String formatXp(long xp) {
        return NumberUtil.formatCoins(xp);
    }

    private static Component separator() {
        return Component.literal(" | ").withStyle(ChatFormatting.DARK_GRAY);
    }

    private static SlayerCategory config() {
        return SkysensConfig.getInstance().slayerCategory;
    }

}
