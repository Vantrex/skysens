package de.vantrex.skysens.client.features.slayer.rng.drop;

import de.vantrex.skysens.client.features.slayer.rng.SlayerRngDrop;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * The drops and minimum tiers follow the Hypixel SkyBlock wiki's RNG meter template, with Paragon Shard sourced from
 * the boss drop page. Paragon Shard's required Slayer XP follows the wiki formula (500 * 100 / highest drop chance);
 * other XP values are estimates (the wiki marks them outdated and its pages disagree), and the actual value is read
 * from the RNG meter menu.
 * The constant names are stored in the config, don't rename them.
 */
@Getter
@RequiredArgsConstructor
public enum VoidgloomRngDropEnum implements SlayerRngDrop {

    NONE("None", 0, 0L),
    BYZANTIUM_DYE("Byzantium Dye", 1, 75_000_000L),
    TWILIGHT_ARROW_POISON("Twilight Arrow Poison", 2, 3_300L),
    ENDERSNAKE_RUNE_I("Endersnake Rune I", 3, 9_438L),
    SUMMONING_EYE("Summoning Eye", 2, 74_250L),
    ENCHANTED_BOOK_MANA_STEAL_I("Enchanted Book (Mana Steal I)", 3, 11_183L),
    TRANSMISSION_TUNER("Transmission Tuner", 3, 22_366L),
    NULL_ATOM("Null Atom", 3, 10_120L),
    HAZMAT_ENDERMAN("Hazmat Enderman", 3, 32_202L),
    POCKET_ESPRESSO_MACHINE("Pocket Espresso Machine", 4, 128_809L),
    ENCHANTED_BOOK_SMARTY_PANTS_I("Enchanted Book (Smarty Pants I)", 4, 28_338L),
    END_RUNE_I("End Rune I", 4, 75_505L),
    HANDY_BLOOD_CHALICE("Handy Blood Chalice", 4, 283_380L),
    SINFUL_DICE("Sinful Dice", 4, 108_992L),
    EXCEEDINGLY_RARE_ENDER_ARTIFACT_UPGRADE("Exceedingly Rare Ender Artifact Upgrade", 4, 1_771_125L),
    VOID_CONQUEROR_ENDERMAN_SKIN("Void Conqueror Enderman Skin", 4, 302_020L),
    ETHERWARP_MERGER("Etherwarp Merger", 4, 118_075L),
    JUDGEMENT_CORE("Judgement Core", 4, 885_562L),
    ENCHANT_RUNE_I("Enchant Rune I", 4, 1_078_642L),
    END_STONE_IDOL("End Stone Idol", 4, 3_542_250L),
    PARAGON_SHARD("Paragon Shard", 4, 200_000L);

    private final String itemName;
    private final int minTier;
    private final long wikiRequiredXp;

    @Override
    public String toString() {
        return SlayerRngDrop.label(this);
    }

}
