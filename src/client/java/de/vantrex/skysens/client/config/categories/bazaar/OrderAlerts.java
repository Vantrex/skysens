package de.vantrex.skysens.client.config.categories.bazaar;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.enums.NotificationDisplayTypeEnum;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class OrderAlerts {

    @ConfigOption(name = "Order Alerts", desc = "Track your open bazaar orders and get alerted when they are outbid, undercut or filled")
    @ConfigEditorBoolean
    @Expose
    public boolean enabled = true;

    @ConfigOption(name = "Outbid alerts", desc = "Alert when someone places a higher buy order than yours")
    @ConfigEditorBoolean
    @Expose
    public boolean outbidAlerts = true;

    @ConfigOption(name = "Undercut alerts", desc = "Alert when someone lists a sell offer below yours")
    @ConfigEditorBoolean
    @Expose
    public boolean undercutAlerts = true;

    @ConfigOption(name = "Filled alerts", desc = "Alert when one of your orders is filled and ready to claim")
    @ConfigEditorBoolean
    @Expose
    public boolean filledAlerts = true;

    @ConfigOption(name = "Check interval (seconds)", desc = "How often the bazaar prices are checked for outbid and undercut orders (one request for all products)")
    @ConfigEditorSlider(minValue = 20f, maxValue = 300f, minStep = 5f)
    @Expose
    public float pollIntervalSeconds = 60f;

    @ConfigOption(name = "Display type", desc = "How the alerts are shown")
    @ConfigEditorDropdown
    @Expose
    public NotificationDisplayTypeEnum displayType = NotificationDisplayTypeEnum.TITLE;

    @ConfigOption(name = "Play sound", desc = "Play a sound with the alerts")
    @ConfigEditorBoolean
    @Expose
    public boolean playSound = true;

    @ConfigOption(name = "Ignore differences below (%)", desc = "Don't alert when the better price is less than this many percent away from yours. 0 alerts on every change.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 10f, minStep = 0.1f)
    @Expose
    public float minimumPercent = 0.1f;

    @ConfigOption(name = "Ignore differences below (coins)", desc = "Don't alert when the better price is less than this many coins away from yours. 0 alerts on every change.")
    @ConfigEditorSlider(minValue = 0f, maxValue = 10000f, minStep = 1f)
    @Expose
    public float minimumCoins = 0f;

    @ConfigOption(name = "Only on SkyBlock", desc = "Only alert while you are on SkyBlock. Otherwise alerts also show as chat messages elsewhere on Hypixel.")
    @ConfigEditorBoolean
    @Expose
    public boolean onlyOnSkyBlock = true;

}
