package de.vantrex.skysens.client.config.migration;

import com.google.gson.JsonObject;
import de.vantrex.skysens.client.config.SkysensConfig;

/**
 * A one-off upgrade of saved config values, e.g. after an option moved to another category and its JSON path changed.
 * Register implementations in {@link ConfigMigrations}.
 */
public interface ConfigMigration {

    /**
     * Whether the saved file still needs this migration.
     *
     * @param root the raw {@code config.json} as it was on disk before MoulConfig loaded it
     */
    boolean shouldMigrate(JsonObject root);

    /**
     * Copies the old values from {@code root} into the freshly loaded {@code config}. Saving is done by the caller.
     *
     * @param root   the raw {@code config.json} as it was on disk before MoulConfig loaded it
     * @param config the loaded config instance
     */
    void migrate(JsonObject root, SkysensConfig config);
}
