package de.vantrex.skysens.client.config.categories.qol;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.config.categories.qol.sub.SensitivitySubCategory;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class QolCategory {

    @ConfigOption(name = "Sensitivity", desc = "Quality of life settings related to sensitivity")
    @Accordion
    @Expose
    public SensitivitySubCategory sensitivity = new SensitivitySubCategory();


}
