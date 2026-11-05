package de.vantrex.skysens.client.features.macro.config;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.config.annotation.ConfigMasterToggle;
import de.vantrex.skysens.client.notification.NotificationLevelEnum;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

/** Conditional macros. Field names are JSON keys, do not rename them. Timings are constants in MacroConstants. */
public class MacroCategory {

    @ConfigOption(name = "Enable Macros", desc = "Let your macro workflows react to key presses on SkyBlock. Without workflows nothing happens")
    @ConfigEditorBoolean
    @ConfigMasterToggle
    @Expose
    public boolean enabled = true;

    @ConfigOption(name = "Several Matches Warning", desc = "Where to warn when a key press matched several branches or workflows (only the first one runs)")
    @ConfigEditorDropdown
    @Expose
    public MatchWarningDisplay multipleMatchWarning = MatchWarningDisplay.CHAT;

    @ConfigOption(name = "Several Matches Warning Level", desc = "Level of the toast when the warning is shown as a toast." + NotificationLevelEnum.LEVELS_DESC)
    @ConfigEditorDropdown
    @Expose
    public NotificationLevelEnum multipleMatchWarningLevel = NotificationLevelEnum.IMPORTANT;

    @ConfigOption(name = "Show Blocked Hint", desc = "Show a short hint above the hotbar when a macro was skipped (rate limit, cooldown or unknown state)")
    @ConfigEditorBoolean
    @Expose
    public boolean showBlockedHint = true;

    @ConfigOption(name = "Execution Log Size", desc = "How many recent key presses the executions log keeps")
    @ConfigEditorSlider(minValue = 10f, maxValue = 200f, minStep = 10f)
    @Expose
    public float executionLogSize = 50f;

    /** Set once the editor tutorial was shown. Not an option; persisted name, never rename. */
    @Expose
    public boolean macroTutorialSeen = false;

}
