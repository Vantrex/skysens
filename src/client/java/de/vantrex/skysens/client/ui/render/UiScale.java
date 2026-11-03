package de.vantrex.skysens.client.ui.render;

import com.mojang.blaze3d.platform.Window;
import de.vantrex.skysens.client.config.SkysensConfig;
import net.minecraft.client.Minecraft;

/**
 * Effective GUI scale of the config screen. The game's GUI scale (often 4 on 1080p/1440p) makes the window fill
 * the screen, so while the screen is open it can render at a smaller integer scale: the screen draws under a pose
 * scale of {@link #factor()} and lays out in the virtual size {@link #width()} x {@link #height()}.
 * <p>
 * Inactive (outside {@code begin}/{@code end}) everything reports the game's own values with factor 1.
 */
public final class UiScale {

    /** Smallest virtual screen the layout is designed for (window max 760x440 minus margins). */
    private static final int MIN_W = 680;
    private static final int MIN_H = 400;

    private static boolean active;
    private static int effective = 1;
    private static float factor = 1f;
    private static int virtualW;
    private static int virtualH;

    private UiScale() {
    }

    /** True when the persisted mode is AUTO (anything but "GAME"). */
    public static boolean isAuto() {
        try {
            return !"GAME".equalsIgnoreCase(SkysensConfig.getInstance().guiScaleMode);
        } catch (RuntimeException ignored) {
            return true; // config unavailable: default mode
        }
    }

    /** Persists the scale mode (saved when the screen closes). */
    public static void setAuto(boolean auto) {
        try {
            SkysensConfig.getInstance().guiScaleMode = auto ? "AUTO" : "GAME";
        } catch (RuntimeException ignored) {
            // persisting is best effort
        }
    }

    /**
     * Activates the effective scale for the current window: in AUTO mode the largest integer scale (up to the
     * game's) that still leaves a usable virtual screen, in GAME mode the game's scale.
     */
    public static void begin() {
        Window window = Minecraft.getInstance().getWindow();
        int game = gameScale(window);
        int scale = game;
        if (isAuto()) {
            scale = 1;
            for (int s = game; s >= 1; s--) {
                if (window.getWidth() / s >= MIN_W && window.getHeight() / s >= MIN_H) {
                    scale = s;
                    break;
                }
            }
        }
        effective = scale;
        factor = (float) scale / game;
        virtualW = (int) (window.getGuiScaledWidth() / factor);
        virtualH = (int) (window.getGuiScaledHeight() / factor);
        active = true;
    }

    /** Back to the game's own scale. */
    public static void end() {
        active = false;
        effective = 1;
        factor = 1f;
    }

    /** Pose scale applied to the screen's rendering and divisor for incoming mouse coordinates (1 when inactive). */
    public static float factor() {
        return active ? factor : 1f;
    }

    /** Integer GUI scale the screen is drawn at (the glyph oversample level follows it). */
    public static int guiScale() {
        return active ? effective : gameScale(Minecraft.getInstance().getWindow());
    }

    /** Virtual screen width in scaled units. */
    public static int width() {
        return active ? virtualW : Minecraft.getInstance().getWindow().getGuiScaledWidth();
    }

    /** Virtual screen height in scaled units. */
    public static int height() {
        return active ? virtualH : Minecraft.getInstance().getWindow().getGuiScaledHeight();
    }

    private static int gameScale(Window window) {
        return Math.max(1, (int) Math.round(window.getGuiScale()));
    }
}
