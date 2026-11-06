package de.vantrex.skysens.client.features.macro.condition;

/** What the player is currently doing. Persisted by name in the activity condition. Never rename. */
public enum MacroActivity {
    DUNGEON("In a dungeon"),
    KUUDRA("In Kuudra"),
    SLAYER("Slayer quest active"),
    DIANA("Doing Diana");

    private final String displayName;

    MacroActivity(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return this.displayName;
    }
}
