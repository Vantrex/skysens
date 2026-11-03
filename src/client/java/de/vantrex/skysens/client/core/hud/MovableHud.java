package de.vantrex.skysens.client.core.hud;

import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * A HUD element that can be moved and scaled in the HUD editor ({@code /skysens hud}). Features expose theirs through
 * {@link de.vantrex.skysens.client.core.feature.HudRenderFeature#movableHuds()}, everything else registers in
 * {@link HudRegistry}. Positions are in GUI-scaled screen pixels, the element decides how it stores them (usually in
 * percent of the screen, so they survive a change of window size or GUI scale). Client thread.
 */
public interface MovableHud {

    /** Shown above the element in the editor. */
    String name();

    /** Whether the element is switched on. Switched off elements are still shown in the editor, marked as disabled. */
    default boolean isEnabled() {
        return true;
    }

    /** Width of the content the editor shows, before scaling. */
    int width();

    /** Height of the content the editor shows, before scaling. */
    int height();

    float scale();

    /** Sets the scale, already clamped to {@link #minScale()}..{@link #maxScale()}. */
    void setScale(float scale);

    default float minScale() {
        return 0.5f;
    }

    default float maxScale() {
        return 2f;
    }

    /** Left edge on a screen of the given GUI-scaled size. */
    float left(int screenWidth, int screenHeight);

    /** Top edge on a screen of the given GUI-scaled size. */
    float top(int screenWidth, int screenHeight);

    /** Moves the element so its top left corner is at the given point. */
    void moveTo(float left, float top, int screenWidth, int screenHeight);

    /** Draws the content with its top left corner at 0, 0 and unscaled; sample content if there is nothing to show. */
    void renderPreview(GuiGraphicsExtractor graphics);

    /** Back to the default position and scale. */
    void reset();

}
