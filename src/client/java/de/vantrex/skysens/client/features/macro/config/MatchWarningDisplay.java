package de.vantrex.skysens.client.features.macro.config;

/** Where the "several branches matched" warning is shown. The constant names are stored in the config, do not rename them. */
public enum MatchWarningDisplay {

    CHAT("Chat"),
    TOAST("Toast"),
    OFF("Off");

    private final String displayName;

    MatchWarningDisplay(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return this.displayName;
    }

}
