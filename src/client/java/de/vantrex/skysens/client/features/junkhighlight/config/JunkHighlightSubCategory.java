package de.vantrex.skysens.client.features.junkhighlight.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class JunkHighlightSubCategory {

    public enum GearRarity {
        COMMON("Common"), UNCOMMON("Uncommon"), RARE("Rare"), EPIC("Epic"), LEGENDARY("Legendary"), MYTHIC("Mythic");

        private final String displayName;

        GearRarity(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    @ConfigOption(name = "Highlight in sell menus", desc = "Highlight junk items in your inventory while an NPC shop (sell menu), Trades or the Booster Cookie menu is open")
    @ConfigEditorBoolean
    @Expose
    public boolean highlightInSellMenus = false;

    @ConfigOption(name = "Highlight in salvage menu", desc = "Highlight junk items in your inventory while the Salvage Items menu is open")
    @ConfigEditorBoolean
    @Expose
    public boolean highlightInSalvageMenu = false;

    @ConfigOption(name = "Highlight colour", desc = "Colour of the junk highlight")
    @ConfigEditorColour
    @Expose
    public ChromaColour highlightColour = ChromaColour.fromStaticRGB(255, 60, 60, 120);

    @ConfigOption(name = "Dungeon potions", desc = "Junk: Dungeon I - VII Potions")
    @ConfigEditorBoolean
    @Expose
    public boolean dungeonPotions = true;

    @ConfigOption(name = "Wisp's Ice-Flavored Water", desc = "Junk: Wisp's Ice-Flavored Water splash potions")
    @ConfigEditorBoolean
    @Expose
    public boolean wispWater = true;

    @ConfigOption(name = "Salvageable dungeon gear", desc = "Junk: dungeon armor and weapons up to the maximum rarity below")
    @ConfigEditorBoolean
    @Expose
    public boolean dungeonGear = false;

    @ConfigOption(name = "Max gear rarity", desc = "Dungeon gear up to this rarity counts as junk")
    @ConfigEditorDropdown
    @Expose
    public GearRarity maxGearRarity = GearRarity.UNCOMMON;

    @ConfigOption(name = "Never highlight recombobulated / starred", desc = "Safety: dungeon gear that is recombobulated or has stars is never highlighted")
    @ConfigEditorBoolean
    @Expose
    public boolean protectUpgradedGear = true;

    @ConfigOption(name = "Extra junk items", desc = "Comma separated item IDs or exact names (e.g. REVIVE_STONE, Hilt of Revelations)")
    @ConfigEditorText
    @Expose
    public String customItems = "";

}
