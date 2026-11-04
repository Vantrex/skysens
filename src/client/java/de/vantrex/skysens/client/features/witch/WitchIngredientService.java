package de.vantrex.skysens.client.features.witch;

import de.vantrex.skysens.client.persistence.RepositoryRegistry;
import de.vantrex.skysens.client.skyblock.player.ProfileService;
import de.vantrex.skysens.client.skyblock.sack.SackItem;
import de.vantrex.skysens.client.util.NumberUtil;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Keeps the amount of each Year of the Witch ingredient in the Witch's Sack of the current account and profile. There
 * is no API for it, so it comes from two places:
 * <ul>
 *     <li>the Witch's Sack menu, whose ingredients show "Stored: 1,234/20.2k" in their lore. It is exact and
 *     overwrites what was counted before, every time the sack is opened;</li>
 *     <li>the sack messages ("[Sacks] +3 items, -1 item."), which add and remove what they list.</li>
 * </ul>
 * Client thread.
 */
public final class WitchIngredientService {

    public static final String MENU_TITLE = "Witch's Sack";
    /** "Stored: 1,234/20.2k" in the lore of an ingredient in the Witch's Sack. */
    private static final Pattern STORED_PATTERN = Pattern.compile("^Stored: (?<amount>[\\d,.]+[kKmM]?)\\s*/.*$");
    /** Extra time before a sack message's window in which a menu read still counts as covering it. */
    private static final long MENU_READ_SLACK_MS = 1_000;
    /** How long a gain is highlighted in the HUD. */
    private static final long HIGHLIGHT_MS = 5_000;

    @Getter
    private static final WitchIngredientService instance = new WitchIngredientService();

    /** When each ingredient was last added to, highlighted until {@link #HIGHLIGHT_MS} after. */
    private final Map<WitchIngredient, Long> lastGainMs = new EnumMap<>(WitchIngredient.class);
    /** When the Witch's Sack menu was last read (wall clock), 0 before the first read this session. */
    private long lastMenuReadMs;

    private WitchIngredientService() {
    }

    /** One item in the open menu: its stripped name and lore. */
    public record MenuItem(@NotNull String name, @NotNull List<String> lore) {
    }

    /**
     * The ingredients of the current profile of the logged in account (by UUID, profiles of different accounts can
     * share a name), by profile id or by name while the id is unknown. Null while the profile is unknown, then nothing
     * is counted.
     */
    public @Nullable WitchIngredientRepository.Profile current() {
        final ProfileService profiles = ProfileService.getInstance();
        final String name = profiles.getCurrentProfile();
        if (name == null) {
            return null;
        }
        return repository().account(Minecraft.getInstance().getUser().getProfileId().toString())
                .profile(profiles.getCurrentProfileId(), name);
    }

    /**
     * Takes the amounts from the open Witch's Sack. Called every time the sack is read while open, so it always
     * reflects the latest menu. Items that aren't ingredients or have no "Stored:" line (buttons, panes) are skipped.
     */
    public void onSackMenu(@NotNull List<MenuItem> items) {
        final WitchIngredientRepository.Profile profile = this.current();
        if (profile == null) {
            return;
        }
        final Map<WitchIngredient, Integer> read = new EnumMap<>(WitchIngredient.class);
        for (final MenuItem item : items) {
            final WitchIngredient ingredient = WitchIngredient.byName(item.name());
            if (ingredient == null) {
                continue;
            }
            for (final String line : item.lore()) {
                final Matcher matcher = STORED_PATTERN.matcher(line.trim());
                if (matcher.matches()) {
                    read.put(ingredient, parseAmount(matcher.group("amount")));
                    break;
                }
            }
        }
        // the menu is still loading, or it isn't the Witch's Sack after all
        if (read.isEmpty()) {
            return;
        }
        this.lastMenuReadMs = System.currentTimeMillis();
        boolean changed = !profile.sackRead;
        for (final Map.Entry<WitchIngredient, Integer> entry : read.entrySet()) {
            final Integer old = profile.stored.put(entry.getKey(), entry.getValue());
            changed |= old == null || !old.equals(entry.getValue());
        }
        profile.sackRead = true;
        if (changed) {
            save();
        }
    }

    /** Adds what went into the Witch's Sack, from a {@link de.vantrex.skysens.client.skyblock.event.SackItemsAddedEvent}. */
    public void onAdded(@NotNull List<SackItem> items, long windowMs) {
        this.apply(items, windowMs, 1);
    }

    /** Removes what was taken out of the Witch's Sack, from a {@link de.vantrex.skysens.client.skyblock.event.SackItemsRemovedEvent}. */
    public void onRemoved(@NotNull List<SackItem> items, long windowMs) {
        this.apply(items, windowMs, -1);
    }

    /**
     * Sack messages sum up the last few seconds, so a change in one may already be in the last menu read. A message
     * whose window reaches back to that read is skipped: counting it again would add a drop twice or take a
     * withdrawal off an amount that already lacks it. What else it held shows up the next time the sack is opened.
     */
    private void apply(List<SackItem> items, long windowMs, int sign) {
        final WitchIngredientRepository.Profile profile = this.current();
        if (profile == null) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (now - windowMs - MENU_READ_SLACK_MS <= this.lastMenuReadMs) {
            return;
        }
        boolean changed = false;
        for (final SackItem item : items) {
            final WitchIngredient ingredient = WitchIngredient.byName(item.name());
            if (ingredient == null) {
                continue;
            }
            profile.stored.put(ingredient, Math.max(0, profile.amount(ingredient) + sign * item.amount()));
            if (sign > 0) {
                this.lastGainMs.put(ingredient, now);
            }
            changed = true;
        }
        if (changed) {
            save();
        }
    }

    /** Whether the ingredient was added to in the last few seconds. */
    public boolean isHighlighted(@NotNull WitchIngredient ingredient) {
        final Long at = this.lastGainMs.get(ingredient);
        return at != null && System.currentTimeMillis() - at < HIGHLIGHT_MS;
    }

    /** "1,234" exactly, or "12.5k" rounded if the menu ever abbreviates it. */
    private static int parseAmount(String text) {
        try {
            return (int) Math.round(NumberUtil.parseAbbreviated(text));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static WitchIngredientRepository repository() {
        return RepositoryRegistry.WITCH_INGREDIENT_REPOSITORY;
    }

    private static void save() {
        RepositoryRegistry.saveRepository(repository());
    }

}
