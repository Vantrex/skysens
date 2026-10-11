package de.vantrex.skysens.client.config.categories.dungeon;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.enums.ShareChannelEnum;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class ChestLootShareAccordion {

    @ConfigOption(name = "Share chest loot", desc = "Post the loot of opened reward chests, e.g. \"Bedrock Chest: Recombobulator 3000, Wisdom II - 12.3M profit\"")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = false;

    @ConfigOption(name = "Channel", desc = "Where to post the chest loot")
    @ConfigEditorDropdown
    @Expose
    public ShareChannelEnum channel = ShareChannelEnum.PARTY;

    @ConfigOption(name = "Minimum profit (millions)", desc = "Only share chests with at least this much profit. 0 shares every chest.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 0.1f)
    @Expose
    public float minimumProfitMillions = 0f;

    @ConfigOption(name = "Show profit", desc = "Add the chest's profit to the message")
    @ConfigEditorBoolean
    @Expose
    public boolean showProfit = true;

}
