package de.vantrex.skysens.client.ui.render;

import java.util.Locale;

/**
 * The selectable GUI themes. Each preset is a colour {@link Palette} plus a layout {@link Style}.
 * All colours are ARGB ints with non-zero alpha.
 */
public enum ThemePreset {

    MIDNIGHT("Midnight",
            new Palette(
                    0x99000000, // backdrop
                    0xF20E0E12, // panel
                    0xFF0A0A0D, // sidebar
                    0xFF24242C, // border
                    0xFF24242C, // divider
                    0xFF15151B, // card
                    0xFF1B1B23, // cardHover
                    0xFF111116, // cardNested
                    0xFFEDEDF2, // text
                    0xFF8A8A99, // textMuted
                    0xFF5A5A66, // textFaint
                    0xFFA855F7, // accent
                    0xFFC084FC, // accentHover
                    0x33A855F7, // accentSoft
                    0xFF2C2C36, // track
                    0xFFFFFFFF, // knob
                    0x40000000, // knobRing
                    0xFF101015, // input
                    0xFFF87171, // danger
                    0x0FFFFFFF, // hoverOverlay
                    0xFF15151B, // popover
                    0xFF24242C  // popoverBorder
            ),
            new Style(true, false, true, 6, 5, 10, 10, 6, 26, 14, 4, 8, false, true, false)),

    /** Pure black surfaces for OLED screens; cards are separated by borders instead of lighter fills. */
    OLED("OLED",
            new Palette(
                    0xCC000000, // backdrop
                    0xFF000000, // panel
                    0xFF000000, // sidebar
                    0xFF1C1C1F, // border
                    0xFF141416, // divider
                    0xFF000000, // card
                    0xFF0C0C0E, // cardHover
                    0xFF000000, // cardNested
                    0xFFF2F2F5, // text
                    0xFF8A8A94, // textMuted
                    0xFF55555E, // textFaint
                    0xFFA855F7, // accent
                    0xFFC084FC, // accentHover
                    0x33A855F7, // accentSoft
                    0xFF26262B, // track
                    0xFFFFFFFF, // knob
                    0x40000000, // knobRing
                    0xFF000000, // input
                    0xFFF87171, // danger
                    0x0FFFFFFF, // hoverOverlay
                    0xFF000000, // popover
                    0xFF2A2A30  // popoverBorder
            ),
            new Style(true, true, true, 6, 5, 10, 10, 6, 26, 14, 4, 8, false, false, false)),

    FLAT("Flat",
            new Palette(
                    0x88000000,
                    0xF5101012,
                    0xF5101012,
                    0xFF27272A,
                    0x1AFFFFFF,
                    0x01FFFFFF,
                    0x0DFFFFFF,
                    0x01FFFFFF,
                    0xFFF4F4F5,
                    0xFFA1A1AA,
                    0xFF52525B,
                    0xFFA855F7,
                    0xFFC084FC,
                    0x26A855F7,
                    0xFF3F3F46,
                    0xFFFAFAFA,
                    0x40000000,
                    0xFF1A1A1E,
                    0xFFF87171,
                    0x0DFFFFFF,
                    0xFF18181B,
                    0xFF3F3F46
            ),
            new Style(false, false, false, 4, 0, 6, 12, 0, 22, 12, 2, 6, false, false, false)),

    GLASS("Glass",
            new Palette(
                    0x55000000,
                    0xB0141420,
                    0x40000000,
                    0x30FFFFFF,
                    0x24FFFFFF,
                    0x30FFFFFF,
                    0x48FFFFFF,
                    0x1CFFFFFF,
                    0xFFF5F5FA,
                    0xFFBDBDD0,
                    0xFFA0A0B8,
                    0xFFB26BFA,
                    0xFFCCA0FF,
                    0x40B26BFA,
                    0x4DFFFFFF,
                    0xFFFFFFFF,
                    0x40000000,
                    0x40000000,
                    0xFFFF8A8A,
                    0x14FFFFFF,
                    0xF21A1A28,
                    0xFF3A3A50
            ),
            new Style(true, false, true, 8, 6, 10, 10, 6, 26, 14, 4, 8, true, true, false)),

    LIGHT("Light",
            new Palette(
                    0x66000000,
                    0xFAF7F7FA,
                    0xFFEFEFF4,
                    0xFFDCDCE4,
                    0xFFE4E4EA,
                    0xFFFFFFFF,
                    0xFFF4F4F8,
                    0xFFF7F7FA,
                    0xFF18181B,
                    0xFF52525B,
                    0xFF8A8FA0,
                    0xFF9333EA,
                    0xFF7E22CE,
                    0x229333EA,
                    0xFFD4D4DC,
                    0xFFFFFFFF,
                    0x40000000,
                    0xFFF4F4F8,
                    0xFFDC2626,
                    0x0A000000,
                    0xFFFFFFFF,
                    0xFFDCDCE4
            ),
            new Style(true, true, true, 6, 5, 10, 10, 6, 26, 14, 4, 8, false, true, true));

    /** Every colour token of a theme. */
    public record Palette(
            int backdrop, int panel, int sidebar, int border, int divider,
            int card, int cardHover, int cardNested,
            int text, int textMuted, int textFaint,
            int accent, int accentHover, int accentSoft,
            int track, int knob, int knobRing, int input, int danger, int hoverOverlay,
            int popover, int popoverBorder) {

        /** Component-wise sRGB interpolation, t in 0..1. */
        public static Palette lerp(float t, Palette a, Palette b) {
            return new Palette(
                    RenderUtil.lerpColor(t, a.backdrop, b.backdrop),
                    RenderUtil.lerpColor(t, a.panel, b.panel),
                    RenderUtil.lerpColor(t, a.sidebar, b.sidebar),
                    RenderUtil.lerpColor(t, a.border, b.border),
                    RenderUtil.lerpColor(t, a.divider, b.divider),
                    RenderUtil.lerpColor(t, a.card, b.card),
                    RenderUtil.lerpColor(t, a.cardHover, b.cardHover),
                    RenderUtil.lerpColor(t, a.cardNested, b.cardNested),
                    RenderUtil.lerpColor(t, a.text, b.text),
                    RenderUtil.lerpColor(t, a.textMuted, b.textMuted),
                    RenderUtil.lerpColor(t, a.textFaint, b.textFaint),
                    RenderUtil.lerpColor(t, a.accent, b.accent),
                    RenderUtil.lerpColor(t, a.accentHover, b.accentHover),
                    RenderUtil.lerpColor(t, a.accentSoft, b.accentSoft),
                    RenderUtil.lerpColor(t, a.track, b.track),
                    RenderUtil.lerpColor(t, a.knob, b.knob),
                    RenderUtil.lerpColor(t, a.knobRing, b.knobRing),
                    RenderUtil.lerpColor(t, a.input, b.input),
                    RenderUtil.lerpColor(t, a.danger, b.danger),
                    RenderUtil.lerpColor(t, a.hoverOverlay, b.hoverOverlay),
                    RenderUtil.lerpColor(t, a.popover, b.popover),
                    RenderUtil.lerpColor(t, a.popoverBorder, b.popoverBorder));
        }
    }

    /**
     * Layout and decoration flags of a theme.
     *
     * @param cards         true: options are boxed cards; false: flat rows separated by hairline dividers
     * @param cardBorders   cards get a 1px border
     * @param inputBorders  inputs (search, dropdown button, value chip) always have a border; otherwise only when active
     * @param windowRadius  corner radius of the window
     * @param radius        corner radius of cards (0 for rows)
     * @param padX          horizontal inner padding of a card/row
     * @param padY          vertical inner padding of a card/row
     * @param cardGap       vertical gap between cards/rows
     * @param toggleW       toggle width
     * @param toggleH       toggle height
     * @param sliderTrackH  slider track thickness
     * @param thumbSize     slider thumb diameter (grows by 2 while hovered)
     * @param glow          soft accent halo around knobs and thumbs
     * @param sidebarBlock  sidebar drawn as its own tinted block (false: same surface as the panel)
     * @param knobRing      subtle ring around knobs (needed on light backgrounds)
     */
    public record Style(boolean cards, boolean cardBorders, boolean inputBorders, int windowRadius, int radius,
                        int padX, int padY, int cardGap, int toggleW, int toggleH, int sliderTrackH,
                        int thumbSize, boolean glow, boolean sidebarBlock, boolean knobRing) {
    }

    private final String displayName;
    private final Palette palette;
    private final Style style;

    ThemePreset(String displayName, Palette palette, Style style) {
        this.displayName = displayName;
        this.palette = palette;
        this.style = style;
    }

    public String displayName() {
        return displayName;
    }

    public Palette palette() {
        return palette;
    }

    public Style style() {
        return style;
    }

    /** Parses a persisted id (case-insensitive); unknown or null ids fall back to {@link #MIDNIGHT}. */
    public static ThemePreset byId(String id) {
        if (id != null) {
            String key = id.trim().toUpperCase(Locale.ROOT);
            for (ThemePreset p : values()) {
                if (p.name().equals(key)) {
                    return p;
                }
            }
        }
        return MIDNIGHT;
    }
}
