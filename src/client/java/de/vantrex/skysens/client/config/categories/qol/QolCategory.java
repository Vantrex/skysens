package de.vantrex.skysens.client.config.categories.qol;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.config.categories.qol.sub.SensitivitySubCategory;
import de.vantrex.skysens.client.config.categories.qol.sub.SwingAnimationSubCategory;
import io.github.notenoughupdates.moulconfig.annotations.Category;

public class QolCategory {

    @Category(name = "Swing Animation", desc = "Quality of life settings related to swing animation")
    @Expose
    public SwingAnimationSubCategory swingAnimation = new SwingAnimationSubCategory();

    @Category(name = "Sensitivity", desc = "Quality of life settings related to sensitivity")
    @Expose
    public SensitivitySubCategory sensitivity = new SensitivitySubCategory();


}
