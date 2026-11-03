package de.vantrex.skysens.client.core.feature;

import net.minecraft.client.CameraType;

public interface CameraFeature extends Feature {

    default CameraType adjustCycledCamera(CameraType next) {
        return next;
    }

    default boolean forceCrosshair() {
        return false;
    }

}
