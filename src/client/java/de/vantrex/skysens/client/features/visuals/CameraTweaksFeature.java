package de.vantrex.skysens.client.features.visuals;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.visuals.config.MiscVisualsAccordion;
import de.vantrex.skysens.client.core.feature.CameraFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import net.minecraft.client.CameraType;

@SkySensFeature
public class CameraTweaksFeature implements CameraFeature {

    private final MiscVisualsAccordion misc = SkysensConfig.getInstance().visualsCategory.misc;

    @Override
    public CameraType adjustCycledCamera(CameraType next) {
        if (this.misc.disableSelfieCam && next == CameraType.THIRD_PERSON_FRONT) {
            return CameraType.FIRST_PERSON;
        }
        return next;
    }

    @Override
    public boolean forceCrosshair() {
        return this.misc.thirdPersonCrosshair;
    }

    @Override
    public boolean isActive() {
        return this.misc.disableSelfieCam || this.misc.thirdPersonCrosshair;
    }
}
