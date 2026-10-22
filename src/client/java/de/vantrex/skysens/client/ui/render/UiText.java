package de.vantrex.skysens.client.ui.render;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.util.Brand;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The only place of the config GUI that measures or draws text. Renders with one of the bundled font families (see {@link FontFamily}; one font
 * definition per family/weight/oversample level, generated at build time, picked from the current GUI scale so glyphs are rasterised at the real pixel
 * density) or with the vanilla font when the user switched the font to Minecraft.
 * <p>
 * Caches (widths, styled components) are valid for one {@link #version()}; the version changes when the font
 * setting or the effective oversample level changes, so callers that cache derived data (wrapped lines, layout
 * widths) should include {@code version()} in their cache key.
 */
public final class UiText {

    private UiText() {
    }

    public enum Weight {
        REGULAR("regular_", 1f),
        SEMIBOLD("semibold_", 1f),
        /**
         * Large semibold text (page title, brand): a dedicated 14px font definition instead of pose-scaling the
         * 9px one, so glyphs are rasterised at native density. Positioned like {@code 1.5x} scaled text; with
         * the Minecraft font it falls back to pose scaling.
         */
        DISPLAY("semibold_display_", DISPLAY_SCALE);

        private final String suffix;
        /** Pose scale used when the vanilla font stands in for this weight. */
        private final float vanillaScale;

        Weight(String suffix, float vanillaScale) {
            this.suffix = suffix;
            this.vanillaScale = vanillaScale;
        }
    }

    /** Visual size ratio of the DISPLAY weight to normal text (14px font vs 9px, ~1.5x). */
    private static final float DISPLAY_SCALE = 1.5f;
    /** The baseline of every font sits 7px below the draw y, so the larger display font is drawn lower. */
    private static final int DISPLAY_Y_OFFSET = 4;

    /** Font definitions exist for oversample levels 1..MAX_LEVEL (= GUI scale). */
    private static final int MAX_LEVEL = 8;
    private static final int CACHE_LIMIT = 2048;

    private static final String FONT_NAMESPACE = "skysens";

    private static FontFamily family = FontFamily.DEFAULT;
    private static boolean loaded;
    private static int level = -1;
    private static int version = 1;

    // Style per weight for the current level (rebuilt when version changes).
    private static final Style[] STYLES = new Style[Weight.values().length];

    private record Entry(Component component, int width) {
    }

    @SuppressWarnings("unchecked")
    private static final Map<String, Entry>[] CACHE = new Map[Weight.values().length];

    static {
        for (int i = 0; i < CACHE.length; i++) {
            CACHE[i] = new HashMap<String, Entry>();
        }
    }

    // ------------------------------------------------------------------ setting / version

    /** The active font family (reads the persisted setting on first use). */
    public static FontFamily family() {
        ensureLoaded();
        return family;
    }

    /** Reads the persisted font setting (call when the screen opens). */
    public static void loadFromConfig() {
        String id = null;
        try {
            id = SkysensConfig.getInstance().guiFont;
        } catch (RuntimeException ignored) {
            // config unavailable: default font
        }
        loaded = true;
        applyFamily(FontFamily.fromId(id));
    }

    /** Switches the font family and persists the choice (saved when the screen closes). */
    public static void setFamily(FontFamily value) {
        ensureLoaded();
        applyFamily(value);
        try {
            SkysensConfig.getInstance().guiFont = value.id();
        } catch (RuntimeException ignored) {
            // persisting is best effort
        }
    }

    private static void applyFamily(FontFamily value) {
        if (family != value) {
            family = value;
            invalidate();
        }
    }

    private static void ensureLoaded() {
        if (!loaded) {
            loadFromConfig();
        }
    }

    private static void invalidate() {
        version++;
        for (Map<String, Entry> m : CACHE) {
            m.clear();
        }
        for (int i = 0; i < STYLES.length; i++) {
            STYLES[i] = null;
        }
    }

    private static int currentLevel() {
        int scale = 1;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.getWindow() != null) {
            scale = (int) Math.round(mc.getWindow().getGuiScale());
        }
        return Math.max(1, Math.min(MAX_LEVEL, scale));
    }

    /**
     * Counter that changes whenever cached text measurements/derived layout must be recomputed (font setting
     * or GUI-scale oversample level changed). Cheap; call as often as needed.
     */
    public static int version() {
        ensureLoaded();
        int lv = currentLevel();
        if (lv != level) {
            level = lv;
            invalidate();
        }
        return version;
    }

    private static Style style(Weight weight) {
        version(); // refresh level / invalidate if needed
        int i = weight.ordinal();
        Style s = STYLES[i];
        if (s == null) {
            s = styleFor(family, weight);
            STYLES[i] = s;
        }
        return s;
    }

    private static final Map<String, Style> STYLE_CACHE = new HashMap<>();

    /** Style selecting the generated font definition of a family/weight at the current level (EMPTY = vanilla). */
    private static Style styleFor(FontFamily f, Weight weight) {
        if (f.vanilla()) {
            return Style.EMPTY;
        }
        String path = f.prefix() + "_" + weight.suffix + level;
        return STYLE_CACHE.computeIfAbsent(path, p -> Style.EMPTY.withFont(
                new FontDescription.Resource(Identifier.fromNamespaceAndPath(FONT_NAMESPACE, p))));
    }

    private static Entry entry(String text, Weight weight) {
        Style style = style(weight);
        Map<String, Entry> cache = CACHE[weight.ordinal()];
        Entry e = cache.get(text);
        if (e == null) {
            Component c = Component.literal(text).setStyle(style);
            int w = font().width(c);
            if (family.vanilla() && weight.vanillaScale != 1f) {
                w = Math.round(w * weight.vanillaScale);
            }
            e = new Entry(c, w);
            if (cache.size() >= CACHE_LIMIT) {
                cache.clear();
            }
            cache.put(text, e);
        }
        return e;
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    // ------------------------------------------------------------------ measuring

    /** Height of one text line in GUI pixels (constant 9, also for the Inter font definitions). */
    public static int lineHeight() {
        return font().lineHeight;
    }

    public static int width(String text) {
        return width(text, Weight.REGULAR);
    }

    public static int width(String text, Weight weight) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return entry(text, weight).width();
    }

    /** Returns {@code text} unchanged if it fits {@code maxWidth}, otherwise cut down with a trailing ellipsis. */
    public static String ellipsize(String text, int maxWidth) {
        return ellipsize(text, maxWidth, Weight.REGULAR);
    }

    public static String ellipsize(String text, int maxWidth, Weight weight) {
        if (text == null || text.isEmpty() || width(text, weight) <= maxWidth) {
            return text == null ? "" : text;
        }
        String dots = family.vanilla() ? "..." : "…";
        int budget = maxWidth - width(dots, weight);
        if (budget <= 0) {
            return "";
        }
        int lo = 0, hi = text.length();
        while (lo < hi) {
            int mid = (lo + hi + 1) >>> 1;
            if (width(text.substring(0, mid), weight) <= budget) {
                lo = mid;
            } else {
                hi = mid - 1;
            }
        }
        if (lo > 0 && Character.isHighSurrogate(text.charAt(lo - 1))) {
            lo--;
        }
        return text.substring(0, lo).stripTrailing() + dots;
    }

    /** Wraps text to {@code maxWidth}; render the lines with {@link #drawLine}. */
    public static List<FormattedCharSequence> wrap(String text, int maxWidth) {
        return wrap(text, maxWidth, Weight.REGULAR);
    }

    public static List<FormattedCharSequence> wrap(String text, int maxWidth, Weight weight) {
        if (text == null || text.isEmpty()) {
            return List.of();
        }
        return font().split(entry(text, weight).component(), Math.max(1, maxWidth));
    }

    // ------------------------------------------------------------------ drawing

    /**
     * Draws text in an explicit font regardless of the current setting (used for the font picker previews).
     * Uncached; only for a handful of strings per frame.
     */
    public static void drawAs(GuiGraphicsExtractor g, String text, FontFamily as, int x, int y, int argb,
                              Weight weight) {
        version();
        g.text(font(), Component.literal(text).setStyle(styleFor(as, weight)), x, y, argb, false);
    }

    public static void draw(GuiGraphicsExtractor g, String text, int x, int y, int argb) {
        draw(g, text, x, y, argb, Weight.REGULAR);
    }

    public static void draw(GuiGraphicsExtractor g, String text, int x, int y, int argb, Weight weight) {
        if (text == null || text.isEmpty()) {
            return;
        }
        if (weight == Weight.DISPLAY) {
            if (!family.vanilla()) {
                g.text(font(), entry(text, weight).component(), x, y + DISPLAY_Y_OFFSET, argb, false);
            } else {
                drawScaled(g, text, x, y, DISPLAY_SCALE, argb, weight);
            }
            return;
        }
        g.text(font(), entry(text, weight).component(), x, y, argb, false);
    }

    /** Draws text in the SkySens brand gradient (see {@link Brand}). Uncached; only for a handful of strings per frame. */
    public static void drawBrand(GuiGraphicsExtractor g, String text, int x, int y, Weight weight) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Component c = Brand.gradient(text, style(weight));
        if (weight == Weight.DISPLAY) {
            if (!family.vanilla()) {
                g.text(font(), c, x, y + DISPLAY_Y_OFFSET, 0xFFFFFFFF, false);
            } else {
                var pose = g.pose();
                pose.pushMatrix();
                pose.translate(x, y);
                pose.scale(DISPLAY_SCALE, DISPLAY_SCALE);
                g.text(font(), c, 0, 0, 0xFFFFFFFF, false);
                pose.popMatrix();
            }
            return;
        }
        g.text(font(), c, x, y, 0xFFFFFFFF, false);
    }

    public static void drawRight(GuiGraphicsExtractor g, String text, int rightX, int y, int argb) {
        drawRight(g, text, rightX, y, argb, Weight.REGULAR);
    }

    public static void drawRight(GuiGraphicsExtractor g, String text, int rightX, int y, int argb, Weight weight) {
        draw(g, text, rightX - width(text, weight), y, argb, weight);
    }

    /** Draws text horizontally centred on {@code centerX}. */
    public static void drawCentered(GuiGraphicsExtractor g, String text, int centerX, int y, int argb) {
        drawCentered(g, text, centerX, y, argb, Weight.REGULAR);
    }

    public static void drawCentered(GuiGraphicsExtractor g, String text, int centerX, int y, int argb,
                                    Weight weight) {
        draw(g, text, centerX - width(text, weight) / 2, y, argb, weight);
    }

    /**
     * Draws text pose-scaled (1.0 = normal) with its top-left at x, y. Soft for scales other than the oversample
     * level; prefer {@link Weight#DISPLAY} for large text. Note {@code width} of DISPLAY is already scaled.
     */
    public static void drawScaled(GuiGraphicsExtractor g, String text, int x, int y, float scale, int argb,
                                  Weight weight) {
        if (text == null || text.isEmpty()) {
            return;
        }
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(scale, scale);
        g.text(font(), entry(text, weight).component(), 0, 0, argb, false);
        pose.popMatrix();
    }

    /** Draws one line produced by {@link #wrap}. */
    public static void drawLine(GuiGraphicsExtractor g, FormattedCharSequence line, int x, int y, int argb) {
        g.text(font(), line, x, y, argb, false);
    }
}
