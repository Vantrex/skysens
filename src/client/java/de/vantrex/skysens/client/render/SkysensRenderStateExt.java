package de.vantrex.skysens.client.render;

/**
 * Duck interface mixed into {@code EntityRenderState} to carry Skysens scale values from extraction to submit.
 */
public interface SkysensRenderStateExt {

    float skysens$getModelScale();

    void skysens$setModelScale(float scale);

    float skysens$getNameTagScale();

    void skysens$setNameTagScale(float scale);

}
