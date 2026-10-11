package de.vantrex.skysens.client.config.categories.chat;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.enums.DropTierEnum;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class LootShareAccordion {

    @ConfigOption(name = "Share rare drops", desc = "Post your rare drops (RARE DROP!, CRAZY RARE DROP!, ...) to co-op chat")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = false;

    @ConfigOption(name = "Minimum rarity", desc = "Only share drops of this rarity or rarer")
    @ConfigEditorDropdown
    @Expose
    public DropTierEnum minimumTier = DropTierEnum.RARE;

    @ConfigOption(name = "Minimum price (millions)", desc = "Only share drops worth at least this many million coins (bazaar instant sell, else lowest BIN). 0 shares every drop.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 0.1f)
    @Expose
    public float minimumPriceMillions = 0f;

    @ConfigOption(name = "Show price", desc = "Add the drop's current value to the message, e.g. \"- 160M coins\" (left out when unknown)")
    @ConfigEditorBoolean
    @Expose
    public boolean showPrice = true;

    @ConfigOption(name = "Share unknown prices", desc = "Share drops whose price can't be determined when a minimum price is set")
    @ConfigEditorBoolean
    @Expose
    public boolean shareUnknownPrice = true;

    @ConfigOption(name = "Share pet drops", desc = "Also share PET DROP! messages")
    @ConfigEditorBoolean
    @Expose
    public boolean petDrops = true;

    @ConfigOption(name = "Share runes", desc = "Also share rune drops (e.g. Lavatears Rune). They still count for the profit tracker.")
    @ConfigEditorBoolean
    @Expose
    public boolean shareRunes = false;

    @ConfigOption(name = "Include Magic Find", desc = "Keep the \"(+123% Magic Find)\" part of the drop message")
    @ConfigEditorBoolean
    @Expose
    public boolean includeMagicFind = true;

}
