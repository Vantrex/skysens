package de.vantrex.skysens.client.features.slayer.rng;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * What is known about the player's slayer RNG meters, saved as {@code repositories/SlayerRngRepository.json}. Don't
 * rename the class or its fields, they are the file name and the JSON keys.
 */
public class SlayerRngRepository {

    /** Per {@link SlayerTypeEnum} constant name. */
    public Map<String, Meter> meters = new HashMap<>();

    public @NotNull Meter meter(@NotNull SlayerTypeEnum type) {
        return this.meters.computeIfAbsent(type.name(), key -> new Meter());
    }

    public static class Meter {
        /** The stored Slayer XP, from the completion message or the RNG meter menu. Null while unknown. */
        public @Nullable Long storedXp;
        /** The meter XP of the last boss, measured between two completion messages. Null while unknown. */
        public @Nullable Long xpPerBoss;
        /** Whether xpPerBoss was measured with Aatrox's Slayer XP Buff active. Null for older measurements. */
        public @Nullable Boolean xpPerBossBuffed;
        /** Drop constant name selected in /rng, "" for no drop, null while the RNG meter menu wasn't read yet. */
        public @Nullable String selectedDrop;
        /** Slayer XP needed per drop constant name, as read from the RNG meter menu. Missing drops use the wiki value. */
        public Map<String, Long> requiredXp = new HashMap<>();
        /** The drop the "meter full" notification was sent for, until the meter is no longer full for it. */
        public @Nullable String notifiedFullDrop;
    }

}
