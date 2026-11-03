package de.vantrex.skysens.client.config.migration;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.vantrex.skysens.client.config.SkysensConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Runs the registered {@link ConfigMigration}s. The raw file has to be read before MoulConfig loads it, because
 * the next save drops keys that no longer exist on the config classes.
 */
public final class ConfigMigrations {

    private static final Logger LOGGER = LoggerFactory.getLogger("skysens");

    private static final List<ConfigMigration> MIGRATIONS = List.of(
            new LegacySwingConfigMigration(),
            new LegacyNotificationLevelsConfigMigration()
    );

    private final JsonObject root;

    private ConfigMigrations(JsonObject root) {
        this.root = root;
    }

    /** Reads the raw config file. Call before the config is loaded. */
    public static ConfigMigrations read(File file) {
        if (!file.isFile()) {
            return new ConfigMigrations(null);
        }
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            return new ConfigMigrations(JsonParser.parseReader(reader).getAsJsonObject());
        } catch (Exception e) {
            LOGGER.warn("Could not inspect config for migrations", e);
            return new ConfigMigrations(null);
        }
    }

    /** Applies every migration that is still needed. Returns whether anything changed and the config should be saved. */
    public boolean apply(SkysensConfig config) {
        if (root == null) {
            return false;
        }
        boolean migrated = false;
        for (ConfigMigration migration : MIGRATIONS) {
            final String name = migration.getClass().getSimpleName();
            try {
                if (!migration.shouldMigrate(root)) {
                    continue;
                }
                migration.migrate(root, config);
                migrated = true;
                LOGGER.info("Applied config migration {}", name);
            } catch (Exception e) {
                LOGGER.warn("Config migration {} failed", name, e);
            }
        }
        return migrated;
    }
}
