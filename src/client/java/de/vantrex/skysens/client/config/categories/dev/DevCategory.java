package de.vantrex.skysens.client.config.categories.dev;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

public class DevCategory {

    @ConfigOption(name = "Debug Mode", desc = "Enable debug mode for additional logging and diagnostics")
    @ConfigEditorBoolean
    @Expose
    public boolean debugMode = false;

    @ConfigOption(name = "Macros Outside SkyBlock", desc = "Let macros fire outside SkyBlock. Only has an effect in the development client")
    @ConfigEditorBoolean
    @Expose
    public boolean macroIgnoreSkyBlock = false;

}
