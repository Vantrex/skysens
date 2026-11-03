package de.vantrex.skysens.client.features.visuals.render;

import lombok.experimental.UtilityClass;

/**
 * Tracks whether render state extraction is currently happening for the world render path
 * ({@code EntityRenderDispatcher#extractEntity}). Other callers of {@code createRenderState}
 * (e.g. the inventory player preview) must not be scaled.
 */
@UtilityClass
public class EntityScaleContext {

    private int depth = 0;

    public void enter() {
        depth++;
    }

    public void exit() {
        if (depth > 0) {
            depth--;
        }
    }

    public boolean isWorldExtract() {
        return depth > 0;
    }

}
