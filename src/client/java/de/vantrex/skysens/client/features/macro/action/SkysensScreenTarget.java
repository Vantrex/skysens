package de.vantrex.skysens.client.features.macro.action;

/** Screens the "open UI" action can open. Persisted by name, never rename. */
public enum SkysensScreenTarget {
    CONFIG("SkySens config"),
    HUD_EDITOR("HUD editor"),
    WARP_MENU("Warp menu"),
    MACRO_EDITOR("Macro editor");

    private final String displayName;

    SkysensScreenTarget(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }
}
