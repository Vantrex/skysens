package de.vantrex.skysens.client.activity;

import de.vantrex.skysens.client.config.categories.chat.ActivityAccordion;

import java.util.function.Predicate;

/** The kinds of activities the {@link ActivityTracker} records, each with its own config toggle. */
public enum ActivityType {

    DUNGEON(config -> config.trackDungeons),
    SLAYER(config -> config.trackSlayers),
    DIANA(config -> config.trackDiana),
    KUUDRA(config -> config.trackKuudra),
    LOCATION(config -> config.trackLocations);

    private final Predicate<ActivityAccordion> enabled;

    ActivityType(Predicate<ActivityAccordion> enabled) {
        this.enabled = enabled;
    }

    public boolean isEnabled(ActivityAccordion config) {
        return this.enabled.test(config);
    }

    /** Whether several entries of this type show a count ("x5"); island visits are always single. */
    public boolean isCountable() {
        return this != LOCATION;
    }

}
