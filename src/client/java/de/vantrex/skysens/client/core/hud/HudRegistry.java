package de.vantrex.skysens.client.core.hud;

import de.vantrex.skysens.client.core.feature.FeatureService;
import de.vantrex.skysens.client.core.feature.HudRenderFeature;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * All HUD elements the HUD editor can move: the ones registered here (HUDs that don't belong to a feature) and the
 * ones of every registered {@link HudRenderFeature}. Client thread.
 */
public final class HudRegistry {

    @Getter
    private static final HudRegistry instance = new HudRegistry();

    private final List<MovableHud> standalone = new ArrayList<>();

    /** True while the HUD editor is open; the live HUD isn't drawn then, the editor draws the previews instead. */
    @Getter
    @Setter
    private boolean editing;

    private HudRegistry() {
    }

    /** Registers a HUD element that doesn't belong to a feature. */
    public void register(MovableHud hud) {
        this.standalone.add(hud);
    }

    /** Every movable HUD element, in registration order. */
    public List<MovableHud> all() {
        final List<MovableHud> huds = new ArrayList<>(this.standalone);
        for (final HudRenderFeature feature : FeatureService.getInstance().getFeatureRegistry().getHudRenderFeature()) {
            huds.addAll(feature.movableHuds());
        }
        return huds;
    }

}
