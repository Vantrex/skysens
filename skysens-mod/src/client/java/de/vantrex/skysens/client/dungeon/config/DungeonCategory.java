package de.vantrex.skysens.client.dungeon.config;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class DungeonCategory {

    @Expose
    @ConfigOption(name = "Boss Waypoints", desc = "Show waypoints for F7 bosses")
    @ConfigEditorBoolean
    public boolean bossWaypoints = true;

    @Expose
    @ConfigOption(name = "Terminal Timer", desc = "Show timers for terminals")
    @ConfigEditorBoolean
    public boolean terminalTimer = true;

}
