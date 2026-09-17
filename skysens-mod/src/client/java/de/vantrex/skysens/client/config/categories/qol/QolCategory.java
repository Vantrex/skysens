package de.vantrex.skysens.client.config.categories.qol;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.config.categories.qol.sub.SensitivitySubCategory;
import de.vantrex.skysens.client.config.categories.qol.sub.SwingAnimationSubCategory;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.Category;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class QolCategory {

    @Category(name = "Swing Animation", desc = "Quality of life settings related to swing animation")
    @Expose
    public SwingAnimationSubCategory swingAnimation = new SwingAnimationSubCategory();

    @ConfigOption(name = "Sensitivity", desc = "Quality of life settings related to sensitivity")
    @Accordion
    @Expose
    public SensitivitySubCategory sensitivity = new SensitivitySubCategory();


}
