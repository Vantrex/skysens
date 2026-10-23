package de.vantrex.skysens.client.features.slayer.rng.drop;

import de.vantrex.skysens.client.features.slayer.rng.SlayerRngDrop;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The drops of the Revenant Horror RNG meter with their minimum tier, from the Hypixel SkyBlock wiki
 * (Template:RNG_Meter/UI/Slayer). Its required Slayer XP is only an estimate (the wiki marks it outdated and its pages
 * disagree), the real value is read from the RNG meter menu. The constant names are stored in the config, don't
 * rename them.
 */
@Getter
@RequiredArgsConstructor
public enum RevenantRngDropEnum implements SlayerRngDrop {

    NONE("None", 0, 0L),
    MATCHA_DYE("Matcha Dye", 1, 75_000_000L),
    FOUL_FLESH("Foul Flesh", 2, 3_093L),
    REVENANT_SHARD("Revenant Shard", 3, 36_742L),
    PESTILENCE_RUNE_I("Pestilence Rune I", 2, 7_977L),
    UNDEAD_CATALYST("Undead Catalyst", 2, 24_750L),
    ENCHANTED_BOOK_SMITE_VI("Enchanted Book (Smite VI)", 3, 124_370L),
    BEHEADED_HORROR("Beheaded Horror", 3, 310_925L),
    REVENANT_CATALYST("Revenant Catalyst", 2, 49_500L),
    SNAKE_RUNE_I("Snake Rune I", 4, 332_250L),
    FESTERING_MAGGOT("Festering Maggot", 4, 367_424L),
    REVENANT_VISCERA("Revenant Viscera", 5, 3_674L),
    SCYTHE_BLADE("Scythe Blade", 4, 489_900L),
    SEVERED_HAND("Severed Hand", 5, 1_049_785L),
    SHREDDED_SINEW("Shredded Sinew", 5, 918_562L),
    WARDEN_HEART("Warden Heart", 5, 3_674_250L);

    private final String itemName;
    private final int minTier;
    private final long wikiRequiredXp;

    @Override
    public String toString() {
        return SlayerRngDrop.label(this);
    }

}
