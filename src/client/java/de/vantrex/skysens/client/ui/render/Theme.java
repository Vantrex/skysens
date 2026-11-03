package de.vantrex.skysens.client.ui.render;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.ui.render.ThemePreset.Palette;
import de.vantrex.skysens.client.ui.render.ThemePreset.Style;
import net.minecraft.util.Util;

/**
 * Colour tokens (ARGB) and layout values of the config GUI. Single source of truth.
 * <p>
 * The tokens are plain statics refreshed by {@link #update()} (call once at the start of each frame).
 * Switching presets with {@link #apply(ThemePreset, boolean)} crossfades the colours over
 * {@link #FADE_MS} ms; style-driven layout values switch immediately.
 */
public final class Theme {

    private Theme() {
    }

    public static final long FADE_MS = 250L;

    // ---- colours (updated by update())
    public static int BACKDROP;
    public static int PANEL;
    public static int SIDEBAR;
    public static int BORDER;
    /** Hairline divider between rows / header / sidebar. */
    public static int DIVIDER;
    public static int CARD;
    public static int CARD_HOVER;
    public static int CARD_NESTED;
    public static int TEXT;
    public static int TEXT_MUTED;
    public static int TEXT_FAINT;
    public static int ACCENT;
    public static int ACCENT_HOVER;
    public static int ACCENT_SOFT;
    public static int TRACK;
    public static int KNOB;
    /** Subtle ring around knobs on light backgrounds. */
    public static int KNOB_RING;
    public static int INPUT;
    public static int DANGER;
    /** Subtle overlay used for hover states. */
    public static int HOVER_OVERLAY;
    /** Solid elevated surface of popovers (dropdown menu, colour picker, theme picker). */
    public static int POPOVER;
    public static int POPOVER_BORDER;

    // ---- style driven layout (switch immediately)
    public static int CARD_RADIUS = 5;
    /** Vertical inner padding of a card/row. */
    public static int CARD_PADDING = 10;
    /** Horizontal inner padding of a card/row. */
    public static int CARD_PADDING_X = 10;
    public static int CARD_GAP = 6;
    public static int WINDOW_RADIUS = 6;

    // ---- fixed layout
    public static final int NESTED_INDENT = 10;
    public static final int CONTENT_PADDING = 12;
    public static final int SIDEBAR_WIDTH = 150;
    public static final int HEADER_HEIGHT = 46;

    private static ThemePreset preset = ThemePreset.MIDNIGHT;
    private static Style style = ThemePreset.MIDNIGHT.style();
    private static Palette current = ThemePreset.MIDNIGHT.palette();
    private static Palette fadeFrom = current;
    private static long fadeStart;
    private static boolean fading;

    static {
        publish(current);
        publishStyle();
    }

    public static ThemePreset preset() {
        return preset;
    }

    public static Style style() {
        return style;
    }

    public static boolean isTransitioning() {
        return fading;
    }

    /** Loads the persisted preset from the config without animation (call when the screen opens). */
    public static void loadFromConfig() {
        String id = null;
        try {
            id = SkysensConfig.getInstance().guiTheme;
        } catch (RuntimeException ignored) {
            // config unavailable: fall back to the default theme
        }
        set(ThemePreset.byId(id), false);
    }

    /** Switches to the preset and persists the choice in the config (saved when the screen closes). */
    public static void apply(ThemePreset target, boolean animate) {
        set(target, animate);
        try {
            SkysensConfig.getInstance().guiTheme = target.name();
        } catch (RuntimeException ignored) {
            // persisting is best effort
        }
    }

    private static void set(ThemePreset target, boolean animate) {
        if (animate && target != preset) {
            update(); // bring "current" up to date so interrupted fades continue from what is on screen
            fadeFrom = current;
            fadeStart = Util.getMillis();
            fading = true;
        } else {
            fading = false;
            current = target.palette();
            publish(current);
        }
        preset = target;
        style = target.style();
        publishStyle();
    }

    /** Advances the crossfade and refreshes the colour tokens. Cheap when nothing is animating. */
    public static void update() {
        if (!fading) {
            return;
        }
        float t = (Util.getMillis() - fadeStart) / (float) FADE_MS;
        if (t >= 1f) {
            fading = false;
            current = preset.palette();
        } else {
            float e = t * t * (3f - 2f * t);
            current = Palette.lerp(e, fadeFrom, preset.palette());
        }
        publish(current);
    }

    private static void publish(Palette p) {
        BACKDROP = p.backdrop();
        PANEL = p.panel();
        SIDEBAR = p.sidebar();
        BORDER = p.border();
        DIVIDER = p.divider();
        CARD = p.card();
        CARD_HOVER = p.cardHover();
        CARD_NESTED = p.cardNested();
        TEXT = p.text();
        TEXT_MUTED = p.textMuted();
        TEXT_FAINT = p.textFaint();
        ACCENT = p.accent();
        ACCENT_HOVER = p.accentHover();
        ACCENT_SOFT = p.accentSoft();
        TRACK = p.track();
        KNOB = p.knob();
        KNOB_RING = p.knobRing();
        INPUT = p.input();
        DANGER = p.danger();
        HOVER_OVERLAY = p.hoverOverlay();
        POPOVER = p.popover();
        POPOVER_BORDER = p.popoverBorder();
    }

    private static void publishStyle() {
        CARD_RADIUS = style.radius();
        CARD_PADDING = style.padY();
        CARD_PADDING_X = style.padX();
        CARD_GAP = style.cardGap();
        WINDOW_RADIUS = style.windowRadius();
    }
}
