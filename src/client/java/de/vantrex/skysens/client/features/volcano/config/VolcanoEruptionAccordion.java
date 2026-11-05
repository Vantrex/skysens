package de.vantrex.skysens.client.features.volcano.config;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.chatcommand.ShareChannelEnum;
import de.vantrex.skysens.client.notification.NotificationLevelEnum;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

/** The Blazing Volcano eruption title on the Crimson Isle, and the same server alert for server hunting. */
public class VolcanoEruptionAccordion {

    @ConfigOption(name = "Eruption Title", desc = "Show a title with the explosivity when the Blazing Volcano on the Crimson Isle starts erupting, and when you join a Crimson Isle server during an eruption. Read from the \"Volcano:\" line in the tab list every 10 seconds. CATACLYSMIC eruptions play a sound")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = true;

    @ConfigOption(name = "Only Cataclysmic", desc = "Only show the title for CATACLYSMIC eruptions, the strongest and rarest ones (about 1.7% of eruptions)")
    @ConfigEditorBoolean
    @Expose
    public boolean onlyCataclysmic = false;

    @ConfigOption(name = "Notification Level", desc = "Level of the eruption and same server titles." + NotificationLevelEnum.LEVELS_DESC)
    @ConfigEditorDropdown
    @Expose
    public NotificationLevelEnum level = NotificationLevelEnum.IMPORTANT;

    @ConfigOption(name = "Announce Eruptions", desc = "Post the eruptions you get a title for in chat, e.g. \"CATACLYSMIC volcano eruption on mini123A\". Each eruption is posted once, also when you rejoin its server")
    @ConfigEditorBoolean
    @Expose
    public boolean announce = false;

    @ConfigOption(name = "Announce Channel", desc = "Where to post the eruptions")
    @ConfigEditorDropdown
    @Expose
    public ShareChannelEnum announceChannel = ShareChannelEnum.PARTY;

    @ConfigOption(name = "Message Players", desc = "Comma separated player names to send the eruption to via /msg, e.g. \"Vantrex, Notch\". Works without Announce Eruptions; leave empty to message nobody")
    @ConfigEditorText
    @Expose
    public String messagePlayers = "";

    @ConfigOption(name = "Same Server Alert", desc = "Show a title when you join a Crimson Isle server you were already on a short time ago, so you can warp again right away while searching for an eruption")
    @ConfigEditorBoolean
    @Expose
    public boolean sameServerAlert = true;

    @ConfigOption(name = "Same Server Time (seconds)", desc = "How long after leaving a Crimson Isle server joining it again counts as the same server")
    @ConfigEditorSlider(minValue = 5, maxValue = 300, minStep = 5)
    @Expose
    public int sameServerSeconds = 30;

}
