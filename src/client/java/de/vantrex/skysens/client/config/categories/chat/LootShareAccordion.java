package de.vantrex.skysens.client.config.categories.chat;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.enums.DropTierEnum;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
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

    @ConfigOption(name = "Share pet drops", desc = "Also share PET DROP! messages")
    @ConfigEditorBoolean
    @Expose
    public boolean petDrops = true;

    @ConfigOption(name = "Include Magic Find", desc = "Keep the \"(+123% Magic Find)\" part of the drop message")
    @ConfigEditorBoolean
    @Expose
    public boolean includeMagicFind = true;

}
