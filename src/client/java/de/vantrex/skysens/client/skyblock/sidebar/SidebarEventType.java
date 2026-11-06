package de.vantrex.skysens.client.skyblock.sidebar;

import java.util.regex.Pattern;

/** Timed events that show a countdown line in the sidebar (patterns match the line without formatting). */
public enum SidebarEventType {
    SPOOKY_FESTIVAL("Spooky Festival", Pattern.compile("^Spooky Festival\\s+(\\d*:?\\d+)$")),
    TRAVELING_ZOO("Traveling Zoo", Pattern.compile("^Traveling Zoo\\s+(\\d*:\\d+)$")),
    NEW_YEAR("New Year", Pattern.compile("^New Year Event!\\s+(.+)$"));

    private final String displayName;
    private final Pattern pattern;

    SidebarEventType(String displayName, Pattern pattern) {
        this.displayName = displayName;
        this.pattern = pattern;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    Pattern pattern() {
        return this.pattern;
    }
}
