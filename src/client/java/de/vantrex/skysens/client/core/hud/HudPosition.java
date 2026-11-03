package de.vantrex.skysens.client.core.hud;

import com.google.gson.annotations.Expose;

/**
 * Saved position of a HUD element that has a built-in default place, e.g. one anchored to the bottom of the screen.
 * Until it is moved in the HUD editor ({@link #moved} false) the element stays at that default place, so it keeps
 * following the window size exactly. Field names are JSON keys, don't rename them.
 */
public class HudPosition {

    /** Whether the element was moved in the HUD editor; {@link #x} and {@link #y} are only used then. */
    @Expose
    public boolean moved;

    /** In percent of the screen width; what point of the element it is (left edge, centre) is up to the element. */
    @Expose
    public float x;

    /** Top edge, in percent of the screen height. */
    @Expose
    public float y;

    @Expose
    public float scale = 1f;

    public void reset() {
        this.moved = false;
        this.x = 0f;
        this.y = 0f;
        this.scale = 1f;
    }

}
