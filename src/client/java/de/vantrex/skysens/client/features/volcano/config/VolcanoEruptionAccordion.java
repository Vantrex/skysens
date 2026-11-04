package de.vantrex.skysens.client.features.volcano.config;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.notification.NotificationLevelEnum;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

/** The Blazing Volcano eruption title on the Crimson Isle. */
public class VolcanoEruptionAccordion {

    @ConfigOption(name = "Eruption Title", desc = "Show a title with the explosivity when the Blazing Volcano on the Crimson Isle starts erupting, and when you join a Crimson Isle server during an eruption. Read from the \"Volcano:\" line in the tab list every 10 seconds")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = true;

    @ConfigOption(name = "Only Cataclysmic", desc = "Only show the title for CATACLYSMIC eruptions, the strongest and rarest ones (about 1.7% of eruptions)")
    @ConfigEditorBoolean
    @Expose
    public boolean onlyCataclysmic = false;

    @ConfigOption(name = "Notification Level", desc = "Level of the eruption title." + NotificationLevelEnum.LEVELS_DESC)
    @ConfigEditorDropdown
    @Expose
    public NotificationLevelEnum level = NotificationLevelEnum.IMPORTANT;

}
