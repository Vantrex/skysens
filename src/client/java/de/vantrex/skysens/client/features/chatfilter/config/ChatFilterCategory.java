package de.vantrex.skysens.client.features.chatfilter.config;

import de.vantrex.skysens.client.config.annotation.ConfigMasterToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

/**
 * Hides spammy chat messages, one toggle per message type. Hidden messages are only removed from the chat,
 * other SkySens features still see them. Everything is off by default.
 */
public class ChatFilterCategory {

    @ConfigOption(name = "Enabled", desc = "Master switch. Messages are only hidden while this is on and their own option is on")
    @ConfigEditorBoolean
    @ConfigMasterToggle
    @Expose
    public boolean enabled = true;

    @ConfigOption(name = "Abilities", desc = "Ability damage and ready messages")
    @Accordion
    @Expose
    public AbilitiesAccordion abilities = new AbilitiesAccordion();

    @ConfigOption(name = "Combat / Kill Combo", desc = "Kill Combo, blessings and radio")
    @Accordion
    @Expose
    public CombatAccordion combat = new CombatAccordion();

    @ConfigOption(name = "Stash / Inventory", desc = "Stash and full inventory messages")
    @Accordion
    @Expose
    public StashAccordion stash = new StashAccordion();

    @ConfigOption(name = "Dungeons / Damage Taken", desc = "Messages about damage you take")
    @Accordion
    @Expose
    public DamageAccordion dungeons = new DamageAccordion();

    @ConfigOption(name = "Misc", desc = "Other spammy messages")
    @Accordion
    @Expose
    public MiscAccordion misc = new MiscAccordion();

    public static class AbilitiesAccordion {

        @ConfigOption(name = "Implosion damage", desc = "Hide \"Your Implosion hit N enemies for N damage.\"")
        @ConfigEditorBoolean
        @Expose
        public boolean implosion = false;

        @ConfigOption(name = "Guided Sheep damage", desc = "Hide \"Your Guided Sheep hit N enemies for N damage.\"")
        @ConfigEditorBoolean
        @Expose
        public boolean guidedSheep = false;

        @ConfigOption(name = "Witherborn damage", desc = "Hide \"Your Witherborn hit N enemies for N damage.\"")
        @ConfigEditorBoolean
        @Expose
        public boolean witherborn = false;

        @ConfigOption(name = "Other ability damage", desc = "Hide \"Your <ability> hit N enemies for N damage.\" for all other abilities (Spirit Pet, Thunderstorm, ...)")
        @ConfigEditorBoolean
        @Expose
        public boolean otherAbilityDamage = false;

        @ConfigOption(name = "Guided Sheep available", desc = "Hide \"Guided Sheep is now available!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean sheepAvailable = false;

        @ConfigOption(name = "Thunderstorm ready", desc = "Hide \"Thunderstorm is ready to use! Press DROP to activate it!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean thunderstormReady = false;

        @ConfigOption(name = "Ragnarok ready", desc = "Hide \"Ragnarok is ready to use! Press DROP to activate it!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean ragnarokReady = false;

        @ConfigOption(name = "Castle of Stone ready", desc = "Hide \"Castle of Stone is ready to use! Press DROP to activate it!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean castleOfStoneReady = false;

        @ConfigOption(name = "Rapid Fire ready", desc = "Hide \"Rapid Fire is ready to use! Press DROP to activate it!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean rapidFireReady = false;

        @ConfigOption(name = "Wish ready", desc = "Hide \"Wish is ready to use! Press DROP to activate it!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean wishReady = false;

        @ConfigOption(name = "Healing Circle available", desc = "Hide \"Healing Circle is now available!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean healingCircleAvailable = false;

        @ConfigOption(name = "Maniac Miner available", desc = "Hide \"Maniac Miner is now available!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean maniacMinerAvailable = false;

        @ConfigOption(name = "Throwing Axe available", desc = "Hide \"Throwing Axe is now available!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean throwingAxeAvailable = false;

        @ConfigOption(name = "Ultimate available", desc = "Hide \"Your <Class> ULTIMATE <ability> is now available!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean ultimateAvailable = false;

        @ConfigOption(name = "Other ability ready messages", desc = "Hide all other \"<ability> is now available!\" and \"<ability> is ready to use! Press DROP to activate it!\" messages")
        @ConfigEditorBoolean
        @Expose
        public boolean abilityReady = false;

        @ConfigOption(name = "Ability on cooldown", desc = "Hide \"This ability is on cooldown for N s.\"")
        @ConfigEditorBoolean
        @Expose
        public boolean abilityCooldown = false;

    }

    public static class CombatAccordion {

        @ConfigOption(name = "Kill Combo Magic Find", desc = "Hide \"+N Kill Combo +N% Magic Find\"")
        @ConfigEditorBoolean
        @Expose
        public boolean killComboMagicFind = false;

        @ConfigOption(name = "Kill Combo coins", desc = "Hide \"+N Kill Combo +N coins per kill\"")
        @ConfigEditorBoolean
        @Expose
        public boolean killComboCoins = false;

        @ConfigOption(name = "Kill Combo Combat Wisdom", desc = "Hide \"+N Kill Combo +N Combat Wisdom\"")
        @ConfigEditorBoolean
        @Expose
        public boolean killComboWisdom = false;

        @ConfigOption(name = "Kill Combo expired", desc = "Hide \"Your Kill Combo has expired! You reached a N Kill Combo!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean killComboExpired = false;

        @ConfigOption(name = "Blessing: Granted you", desc = "Hide \"Granted you +...\" lines")
        @ConfigEditorBoolean
        @Expose
        public boolean blessingGranted = false;

        @ConfigOption(name = "Blessing: Also granted you", desc = "Hide \"Also granted you +...\" lines")
        @ConfigEditorBoolean
        @Expose
        public boolean blessingAlsoGranted = false;

        @ConfigOption(name = "Weak radio", desc = "Hide \"Your radio is weak. Find another enjoyer to boost it.\"")
        @ConfigEditorBoolean
        @Expose
        public boolean radioWeak = false;

    }

    public static class StashAccordion {

        @ConfigOption(name = "Items stashed", desc = "Hide \"You have N items stashed away!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean stashItems = false;

        @ConfigOption(name = "Materials stashed", desc = "Hide \"You have N materials stashed away!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean stashMaterials = false;

        @ConfigOption(name = "Stash material types", desc = "Hide \"(This totals N types of materials stashed!)\"")
        @ConfigEditorBoolean
        @Expose
        public boolean stashTypes = false;

        @ConfigOption(name = "Pick up prompt", desc = "Hide \">>> CLICK HERE to pick them up! <<<\"")
        @ConfigEditorBoolean
        @Expose
        public boolean stashClick = false;

        @ConfigOption(name = "SkyHanni stash items", desc = "Hide SkyHanni's \"You have N items in stash...\" message")
        @ConfigEditorBoolean
        @Expose
        public boolean stashSkyHanniItems = false;

        @ConfigOption(name = "SkyHanni stash materials", desc = "Hide SkyHanni's \"You have N materials in stash, totalling N types...\" message")
        @ConfigEditorBoolean
        @Expose
        public boolean stashSkyHanniMaterials = false;

        @ConfigOption(name = "Inventory full", desc = "Hide \"Your inventory is full!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean inventoryFull = false;

    }

    public static class DamageAccordion {

        @ConfigOption(name = "Crypt Wither Skull", desc = "Hide \"A Crypt Wither Skull exploded, hitting you for N damage.\"")
        @ConfigEditorBoolean
        @Expose
        public boolean cryptWitherSkull = false;

        @ConfigOption(name = "Maxor's Frenzy", desc = "Hide \"Maxor's Frenzy hit you for N damage.\"")
        @ConfigEditorBoolean
        @Expose
        public boolean maxorFrenzy = false;

        @ConfigOption(name = "Other damage taken", desc = "Hide other \"<source> hit you for N damage.\" messages (traps, Noxious Ash Cloud, ...)")
        @ConfigEditorBoolean
        @Expose
        public boolean otherDamageTaken = false;

        @ConfigOption(name = "Odin dragon spawned", desc = "Hide Odin's \"<colour> dragon spawned\" message")
        @ConfigEditorBoolean
        @Expose
        public boolean odinDragonSpawned = false;

        @ConfigOption(name = "Odin dragon counts", desc = "Hide Odin's \"<colour> dragon counts.\" message")
        @ConfigEditorBoolean
        @Expose
        public boolean odinDragonCounts = false;

    }

    public static class MiscAccordion {

        @ConfigOption(name = "GEXP", desc = "Hide \"You earned N GEXP from playing SkyBlock!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean gexp = false;

        @ConfigOption(name = "NPC sales", desc = "Hide \"You sold <item> xN for N Coins!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean npcSell = false;

        @ConfigOption(name = "Salvage header", desc = "Hide \"You salvaged N items for:\"")
        @ConfigEditorBoolean
        @Expose
        public boolean salvage = false;

        @ConfigOption(name = "Blocks in the way", desc = "Hide \"There are blocks in the way!\"")
        @ConfigEditorBoolean
        @Expose
        public boolean blocksInTheWay = false;

        @ConfigOption(name = "Odin strength", desc = "Hide Odin's \"Gained strength: N\" message")
        @ConfigEditorBoolean
        @Expose
        public boolean odinStrength = false;

    }

}
