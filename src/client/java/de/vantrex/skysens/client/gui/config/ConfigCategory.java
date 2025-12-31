package de.vantrex.skysens.client.gui.config;

import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a category in the config menu.
 * Can contain sub-categories and config entries.
 */
@Getter
public class ConfigCategory {

    private final String name;
    private final List<ConfigCategory> subCategories = new ArrayList<>();
    private final List<ConfigEntry> entries = new ArrayList<>();

    public ConfigCategory(String name) {
        this.name = name;
    }

    public void addSubCategory(ConfigCategory category) {
        this.subCategories.add(category);
    }

    public void addEntry(ConfigEntry entry) {
        this.entries.add(entry);
    }

    /**
     * Finds all entries in this category and all sub-categories that match the query.
     * 
     * @param query The search query.
     * @return A list of matching entries.
     */
    public List<ConfigEntry> searchEntries(String query) {
        List<ConfigEntry> matches = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (ConfigEntry entry : entries) {
            if (entry.getName().toLowerCase().contains(lowerQuery)) {
                matches.add(entry);
                continue;
            }
            for (String keyword : entry.getSearchKeywords()) {
                if (keyword.toLowerCase().contains(lowerQuery)) {
                    matches.add(entry);
                    break;
                }
            }
        }

        for (ConfigCategory subCategory : subCategories) {
            matches.addAll(subCategory.searchEntries(query));
        }

        return matches;
    }
}
