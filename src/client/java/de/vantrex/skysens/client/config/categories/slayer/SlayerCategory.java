package de.vantrex.skysens.client.config.categories.slayer;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.Accordion;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class SlayerCategory {

    @ConfigOption(name = "Track Slayers", desc = "Track bosses killed, failed quests and kill times per boss this session (shown by !slayer, which also needs its toggle in Chat Commands)")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = true;

    @ConfigOption(name = "Summary Every N Bosses", desc = "Show a summary in your own chat every N killed bosses. 0 = off")
    @ConfigEditorSlider(minValue = 0f, maxValue = 100f, minStep = 1f)
    @Expose
    public float summaryEveryBosses = 0f;

    @ConfigOption(name = "Summary Fields", desc = "Which values !slayer and the chat summary include")
    @Accordion
    @Expose
    public SlayerFieldsAccordion fields = new SlayerFieldsAccordion();

}
