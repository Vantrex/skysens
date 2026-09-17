package de.vantrex.skysens.client.config.categories.server;

import com.google.gson.annotations.Expose;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.annotations.ConfigOption;

/**
 * Toggles for the optional Skysens backend.
 *
 * <p><strong>Declarations only — nothing reads these yet.</strong> They exist so
 * the opt-in surface is visible and reviewable before any code can send data.
 *
 * <p>Every field defaults to off. Offline-first is a hard requirement: with the
 * server disabled, unreachable, or simply never configured, every existing
 * feature must behave exactly as it does today. Anything that leaves the user's
 * machine is opt-in by default (§8).
 */
public class ServerCategory {

    @ConfigOption(name = "Enable Skysens Server", desc = "Master switch. When off, the mod never makes a network request.")
    @ConfigEditorBoolean
    @Expose
    public boolean serverEnabled = false;

    @ConfigOption(name = "Server Base URL", desc = "Base URL of the Skysens backend, e.g. https://api.example.org")
    @ConfigEditorText
    @Expose
    public String serverBaseUrl = "";

    @ConfigOption(name = "Share Dungeon Runs", desc = "Upload completed dungeon runs to build personal bests. Off by default.")
    @ConfigEditorBoolean
    @Expose
    public boolean shareDungeonRuns = false;

    @ConfigOption(name = "Share Sensitivity Profiles", desc = "Allow publishing your sensitivity profiles so others can import them. Off by default.")
    @ConfigEditorBoolean
    @Expose
    public boolean shareSensitivityProfiles = false;

    @ConfigOption(name = "Download Remote Assets", desc = "Fetch updated location and split data without waiting for a mod release.")
    @ConfigEditorBoolean
    @Expose
    public boolean downloadRemoteAssets = false;

    @ConfigOption(name = "Honour Remote Feature Flags", desc = "Let the server disable a feature remotely if it is found to be broken.")
    @ConfigEditorBoolean
    @Expose
    public boolean honourRemoteFeatureFlags = false;
}
