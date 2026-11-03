package de.vantrex.skysens.client.config.categories.qol;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.core.hud.HudPosition;
import de.vantrex.skysens.client.features.junkhighlight.config.JunkHighlightSubCategory;
import de.vantrex.skysens.client.features.sensitivity.config.SensitivitySubCategory;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class QolCategory {

    @ConfigOption(name = "Sensitivity", desc = "Quality of life settings related to sensitivity")
    @Accordion
    @Expose
    public SensitivitySubCategory sensitivity = new SensitivitySubCategory();

    @ConfigOption(name = "Junk Highlight", desc = "Highlight junk items in sell and salvage menus (highlight only, never clicks)")
    @Accordion
    @Expose
    public JunkHighlightSubCategory junkHighlight = new JunkHighlightSubCategory();

    @ConfigOption(name = "Hide Armor Bar", desc = "Hide the vanilla armor bar above the health bar")
    @ConfigEditorBoolean
    @Expose
    public boolean hideArmorBar = false;

    @ConfigOption(name = "Action Bar Above Inventory HUD", desc = "If Inventory HUD+ is installed and its inventory HUD is shown where the action bar is, move the action bar (and SkySens' own) up above it. Nothing moves if they don't overlap.")
    @ConfigEditorBoolean
    @Expose
    public boolean moveActionBarAboveInventoryHud = true;

    @Expose
    public HudPosition secondaryActionBarPosition = new HudPosition();
}
