package de.vantrex.skysens.client.config.migration;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.visuals.config.SwingAnimationAccordion;

/** Swing animation settings moved from qolCategory.swingAnimation to visualsCategory.swingAnimation. */
public class LegacySwingConfigMigration implements ConfigMigration {

    @Override
    public boolean shouldMigrate(JsonObject root) {
        if (root.get("visualsCategory") instanceof JsonObject visuals && visuals.has("swingAnimation")) {
            return false;
        }
        return root.get("qolCategory") instanceof JsonObject qol && qol.get("swingAnimation") instanceof JsonObject;
    }

    @Override
    public void migrate(JsonObject root, SkysensConfig config) {
        final JsonObject legacySwing = root.getAsJsonObject("qolCategory").getAsJsonObject("swingAnimation");
        final SwingAnimationAccordion old = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create()
                .fromJson(legacySwing, SwingAnimationAccordion.class);
        old.clamp();
        config.visualsCategory.swingAnimation.copyFrom(old);
    }
}
