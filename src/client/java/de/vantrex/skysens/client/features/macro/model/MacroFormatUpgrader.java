package de.vantrex.skysens.client.features.macro.model;

import com.google.gson.JsonObject;

/** Upgrades older macro JSON step by step (v1 to v2, v2 to v3, ...) before it is parsed. Local to the macro format. */
public final class MacroFormatUpgrader {

    private MacroFormatUpgrader() {
    }

    /** Upgrades the root object in place to {@link MacroJson#CURRENT_VERSION}. */
    public static void upgrade(JsonObject root, int fromVersion) {
        int version = fromVersion;
        while (version < MacroJson.CURRENT_VERSION) {
            // Add "case N -> upgradeNToN1(root)" here when the format changes.
            version++;
        }
        root.addProperty("version", MacroJson.CURRENT_VERSION);
    }

}
