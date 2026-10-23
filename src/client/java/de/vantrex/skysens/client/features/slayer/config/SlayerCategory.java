package de.vantrex.skysens.client.features.slayer.config;

import de.vantrex.skysens.client.config.annotation.ConfigMasterToggle;
import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class SlayerCategory {

    @ConfigOption(name = "Track Slayers", desc = "Track bosses killed, failed quests and kill times per boss this session (shown by !slayer, which also needs its toggle in Chat Commands)")
    @ConfigEditorBoolean
    @ConfigMasterToggle
    @Expose
    public boolean enabled = true;

    @ConfigOption(name = "Summary Every N Bosses", desc = "Show a summary in your own chat every N killed bosses. 0 = off")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    @Expose
    public float summaryEveryBosses = 0f;

    @ConfigOption(name = "Send Summary to Co-op Chat", desc = "Also send the summary every N bosses to your co-op chat (/cchat), not just your own chat")
    @ConfigEditorBoolean
    @Expose
    public boolean summaryToCoopChat = false;

    @ConfigOption(name = "Idle Pause (seconds)", desc = "Pause the quest and kill timers (bosses per hour, average times) after this long without input (moving, looking around, clicking). Shorter breaks still count")
    @ConfigEditorSlider(minValue = 5f, maxValue = 300f, minStep = 5f)
    @Expose
    public float idlePauseSeconds = 30f;

    @ConfigOption(name = "No Progress Pause (seconds)", desc = "Pause the quest timer after this long without slayer quest progress in the sidebar, before the boss spawns. 0 = off")
    @ConfigEditorSlider(minValue = 0f, maxValue = 300f, minStep = 5f)
    @Expose
    public float noProgressPauseSeconds = 60f;

    @ConfigOption(name = "Profit Idle Pause (minutes)", desc = "Time without input after which questing no longer counts for slayer profit per hour")
    @ConfigEditorSlider(minValue = 1f, maxValue = 30f, minStep = 1f)
    @Expose
    public float profitIdlePauseMinutes = 5f;

    @ConfigOption(name = "Summary Fields", desc = "Which values !slayer and the chat summary include")
    @Accordion
    @Expose
    public SlayerFieldsAccordion fields = new SlayerFieldsAccordion();

}
