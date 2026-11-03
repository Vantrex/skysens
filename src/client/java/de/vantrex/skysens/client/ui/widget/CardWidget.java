package de.vantrex.skysens.client.ui.widget;

import de.vantrex.skysens.client.ui.model.OptionNode;
import de.vantrex.skysens.client.ui.render.Animated;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiScale;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Util;

import java.util.List;

/**
 * Card chrome shared by all leaf widgets: rounded background with eased hover colour, optional faint
 * breadcrumb, name, wrapped description, and a right-hand control slot that subclasses fill.
 * <p>
 * Layout: padding {@link Theme#CARD_PADDING} (vertical) / {@link Theme#CARD_PADDING_X} (horizontal); text column
 * width = {@code width - controlWidth() - 3 * horizontal padding}; the active theme style decides between
 * boxed cards and flat rows with hairline dividers;
 * the control sits at the right edge, vertically centred; the text block is vertically centred as well.
 * When the card is too narrow for a usable text column, the control is stacked below the text instead.
 * <p>
 * Options with a known default reserve a small slot left of the control for a reset button, which is drawn
 * only while the value differs from the default (faint, fully visible while the card is hovered). Options whose
 * container has a master toggle that is off get a translucent card-coloured veil.
 */
public abstract class CardWidget extends OptionWidget {

    /** Below this text column width the control is stacked under the text. */
    private static final int MIN_SIDE_TEXT_WIDTH = 60;
    private static final int STACK_GAP = 4;
    /** Width reserved left of the control for the reset button, and the size of its glyph box. */
    private static final int RESET_SLOT = 14;
    private static final int RESET_SIZE = 10;
    private static final float DIM_ALPHA = 0.55f;
    private static final long CONFIRM_DELAY_MS = 3000;
    private static final int CONFIRM_PAD = 8;
    private static final int CONFIRM_BTN_H = 16;
    private static final int CONFIRM_BTN_GAP = 6;

    private final Animated hover = new Animated(0f, 120f);
    private List<FormattedCharSequence> descLines = List.of();
    private int cachedTextWidth = -1;
    private int cachedTextVersion = -1;
    private String cachedDescription;
    private Boolean hasDefault;
    private boolean confirmOpen;
    private long confirmOpenedAt;

    /** Whether the nested background colour is used (card inside an expanded accordion). */
    protected boolean nested;

    protected CardWidget(OptionNode node) {
        super(node);
    }

    /** Width of the control slot in pixels. */
    protected abstract int controlWidth();

    /** Height of the control in pixels. */
    protected abstract int controlHeight();

    /**
     * Draws the control.
     *
     * @param cx top-left x of the control slot
     * @param cy top-left y of the control slot (already vertically centred)
     */
    protected abstract void renderControl(GuiGraphicsExtractor g, int cx, int cy, int mouseX, int mouseY,
                                          float partialTick);

    /** Hook for subclasses to drop transient state (drags, popovers) after the value was reset to its default. */
    protected void onReset() {
    }

    /** Whether the control is placed below the text (narrow card) instead of to its right. */
    private boolean stacked() {
        return width - controlWidth() - 3 * Theme.CARD_PADDING_X < MIN_SIDE_TEXT_WIDTH;
    }

    /** Left x of the control slot. */
    protected int controlX() {
        if (stacked()) {
            return x + Theme.CARD_PADDING_X;
        }
        return x + width - Theme.CARD_PADDING_X - controlWidth();
    }

    /** Top y of the control slot. */
    protected int controlY() {
        if (stacked()) {
            return y + Theme.CARD_PADDING + textBlockHeight() + STACK_GAP;
        }
        return y + (height() - controlHeight()) / 2;
    }

    public void setNested(boolean nested) {
        this.nested = nested;
    }

    private int textWidth() {
        if (stacked()) {
            return Math.max(20, width - 2 * Theme.CARD_PADDING_X);
        }
        return width - controlWidth() - 3 * Theme.CARD_PADDING_X - resetReserve();
    }

    /** Whether the option has a default value to reset to; checked once, so the layout never jumps. */
    private boolean hasDefault() {
        if (hasDefault == null) {
            hasDefault = node.defaultValue().isPresent();
        }
        return hasDefault;
    }

    private int resetReserve() {
        return hasDefault() && !stacked() ? RESET_SLOT : 0;
    }

    private boolean resetShown() {
        return hasDefault() && !node.isDefault();
    }

    private int resetX() {
        if (stacked()) {
            return x + width - Theme.CARD_PADDING_X - RESET_SIZE;
        }
        return controlX() - RESET_SLOT + (RESET_SLOT - RESET_SIZE) / 2;
    }

    private int resetY() {
        return controlY() + (controlHeight() - RESET_SIZE) / 2;
    }

    private boolean overReset(double mx, double my) {
        return RenderUtil.inside(mx, my, resetX() - 1, resetY() - 1, RESET_SIZE + 2, RESET_SIZE + 2);
    }

    private void resetToDefault() {
        node.resetToDefault();
        onReset();
        playClick();
    }

    @Override
    public void notifyReset() {
        onReset();
    }

    // ------------------------------------------------------------------ reset confirmation popover

    /** Muted "Default: ..." text for simple values, or null when there is no readable form. */
    private String defaultLabel() {
        Object def = node.defaultValue().orElse(null);
        if (def instanceof Boolean b) {
            return "Default: " + (b ? "On" : "Off");
        }
        if (def instanceof Enum<?> en) {
            return "Default: " + UiText.enumLabel(en);
        }
        if (def instanceof Float || def instanceof Double) {
            return "Default: " + new java.math.BigDecimal(((Number) def).doubleValue())
                    .setScale(2, java.math.RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        }
        if (def instanceof Number n) {
            return "Default: " + n;
        }
        return null;
    }

    private long confirmRemainingMs() {
        return ConfirmButtons.remainingMs(confirmOpenedAt, CONFIRM_DELAY_MS);
    }

    /** Popover bounds as {x, y, w, h}. */
    private int[] confirmBounds() {
        String def = defaultLabel();
        int w = Math.max(UiText.width("Reset to default?", UiText.Weight.SEMIBOLD),
                def == null ? 0 : UiText.width(def)) + 2 * CONFIRM_PAD;
        w = Math.max(w, 128);
        int lh = UiText.lineHeight();
        int h = CONFIRM_PAD + lh + (def == null ? 0 : lh + 2) + 6 + CONFIRM_BTN_H + CONFIRM_PAD;
        int sw = UiScale.width();
        int sh = UiScale.height();
        int px = Math.max(4, Math.min(resetX() - 2, sw - w - 4));
        int py = resetY() + RESET_SIZE + 4;
        if (py + h > sh - 4) {
            int above = resetY() - 4 - h;
            py = above >= 4 ? above : Math.max(4, sh - 4 - h);
        }
        return new int[]{px, py, w, h};
    }

    /** Button bounds as {x, y, w, h}; index 0 is Cancel, 1 is Reset. */
    private int[] confirmButton(int[] p, int index) {
        return ConfirmButtons.button(p, index, CONFIRM_PAD, CONFIRM_BTN_H, CONFIRM_BTN_GAP);
    }

    @Override
    public void renderOverlay(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        if (!confirmOpen) {
            return;
        }
        int[] p = confirmBounds();
        RenderUtil.shadow(g, p[0], p[1], p[2], p[3], 5);
        RenderUtil.roundedRectWithBorder(g, p[0], p[1], p[2], p[3], 5, Theme.POPOVER, Theme.POPOVER_BORDER);
        int lh = UiText.lineHeight();
        int ty = p[1] + CONFIRM_PAD;
        UiText.draw(g, "Reset to default?", p[0] + CONFIRM_PAD, ty, Theme.TEXT, UiText.Weight.SEMIBOLD);
        String def = defaultLabel();
        if (def != null) {
            UiText.draw(g, def, p[0] + CONFIRM_PAD, ty + lh + 2, Theme.TEXT_MUTED);
        }
        ConfirmButtons.drawCancel(g, confirmButton(p, 0), mouseX, mouseY);
        ConfirmButtons.drawDanger(g, confirmButton(p, 1), "Reset", confirmRemainingMs(), mouseX, mouseY);
    }

    @Override
    public boolean hasOverlay() {
        return confirmOpen;
    }

    @Override
    public boolean overlayContains(double mx, double my) {
        if (!confirmOpen) {
            return false;
        }
        int[] p = confirmBounds();
        return RenderUtil.inside(mx, my, p[0], p[1], p[2], p[3]);
    }

    @Override
    public void closeOverlay() {
        confirmOpen = false;
    }

    private boolean confirmClicked(double mx, double my, int button) {
        int[] p = confirmBounds();
        if (!RenderUtil.inside(mx, my, p[0], p[1], p[2], p[3])) {
            confirmOpen = false;
            return false;
        }
        if (button != 0) {
            return true;
        }
        int[] cancel = confirmButton(p, 0);
        int[] reset = confirmButton(p, 1);
        if (RenderUtil.inside(mx, my, cancel[0], cancel[1], cancel[2], cancel[3])) {
            confirmOpen = false;
            playClick();
        } else if (RenderUtil.inside(mx, my, reset[0], reset[1], reset[2], reset[3])
                && confirmRemainingMs() == 0) {
            confirmOpen = false;
            resetToDefault();
        }
        return true;
    }

    /** Draws a small circular arrow centred in the reset box. */
    private void drawResetIcon(GuiGraphicsExtractor g, boolean cardHovered, boolean iconHovered) {
        int rx = resetX();
        int ry = resetY();
        if (iconHovered) {
            RenderUtil.roundedRect(g, rx - 1, ry - 1, RESET_SIZE + 2, RESET_SIZE + 2, 3, Theme.HOVER_OVERLAY);
        }
        int colour = iconHovered ? Theme.ACCENT : cardHovered ? Theme.TEXT_MUTED : Theme.TEXT_FAINT;
        double cx = rx + RESET_SIZE / 2.0;
        double cy = ry + RESET_SIZE / 2.0;
        double r = 3.2;
        // open circle with the gap at the top, an arrow head on its left end
        for (int deg = -60; deg <= 240; deg += 10) {
            double a = Math.toRadians(deg);
            int px = (int) Math.floor(cx + r * Math.cos(a));
            int py = (int) Math.floor(cy + r * Math.sin(a));
            g.fill(px, py, px + 1, py + 1, colour);
        }
        double a = Math.toRadians(240);
        int hx = (int) Math.floor(cx + r * Math.cos(a));
        int hy = (int) Math.floor(cy + r * Math.sin(a));
        g.fill(hx - 1, hy - 1, hx + 3, hy, colour);
        g.fill(hx, hy - 2, hx + 1, hy + 1, colour);
    }

    private List<FormattedCharSequence> descriptionLines() {
        int tw = textWidth();
        String desc = node.description();
        int ver = UiText.version();
        if (tw != cachedTextWidth || ver != cachedTextVersion || !desc.equals(cachedDescription)) {
            cachedTextWidth = tw;
            cachedTextVersion = ver;
            cachedDescription = desc;
            descLines = desc.isEmpty() ? List.of() : UiText.wrap(desc, tw);
        }
        return descLines;
    }

    private int breadcrumbHeight() {
        return showBreadcrumb ? UiText.lineHeight() + 2 : 0;
    }

    private int textBlockHeight() {
        int lh = UiText.lineHeight();
        int h = breadcrumbHeight() + lh;
        List<FormattedCharSequence> lines = descriptionLines();
        if (!lines.isEmpty()) {
            h += 3 + lines.size() * (lh + 1) - 1;
        }
        return h;
    }

    @Override
    public int height() {
        if (stacked()) {
            return textBlockHeight() + STACK_GAP + controlHeight() + 2 * Theme.CARD_PADDING;
        }
        return Math.max(controlHeight(), textBlockHeight()) + 2 * Theme.CARD_PADDING;
    }

    @Override
    public final void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int h = height();
        hover.target(isHovered(mouseX, mouseY) ? 1f : 0f);
        int base = nested ? Theme.CARD_NESTED : Theme.CARD;
        int bg = RenderUtil.lerpColor(hover.value(), base, Theme.CARD_HOVER);
        var style = Theme.style();
        if (!style.cards()) {
            // flat row: faint full-width hover tint and a hairline divider
            RenderUtil.rect(g, x, y, width, h, bg);
            RenderUtil.rect(g, x, y + h - 1, width, 1, Theme.DIVIDER);
        } else if (style.cardBorders()) {
            RenderUtil.roundedRectWithBorder(g, x, y, width, h, Theme.CARD_RADIUS, bg, Theme.BORDER);
        } else {
            RenderUtil.roundedRect(g, x, y, width, h, Theme.CARD_RADIUS, bg);
        }

        int lh = UiText.lineHeight();
        int ty = stacked() ? y + Theme.CARD_PADDING : y + (h - textBlockHeight()) / 2;
        int tx = x + Theme.CARD_PADDING_X;
        int tw = textWidth();
        if (showBreadcrumb) {
            UiText.draw(g, UiText.ellipsize(node.breadcrumb(), tw), tx, ty, Theme.TEXT_FAINT);
            ty += breadcrumbHeight();
        }
        UiText.draw(g, UiText.ellipsize(node.name(), tw, UiText.Weight.SEMIBOLD), tx, ty, Theme.TEXT,
                UiText.Weight.SEMIBOLD);
        ty += lh + 3;
        for (FormattedCharSequence line : descriptionLines()) {
            UiText.drawLine(g, line, tx, ty, Theme.TEXT_MUTED);
            ty += lh + 1;
        }

        renderControl(g, controlX(), controlY(), mouseX, mouseY, partialTick);
        if (resetShown()) {
            drawResetIcon(g, hover.value() > 0.5f, overReset(mouseX, mouseY));
        }
        if (node.isDimmed()) {
            int veil = RenderUtil.withAlpha(base, DIM_ALPHA);
            RenderUtil.roundedRect(g, x, y, width, h, style.cards() ? Theme.CARD_RADIUS : 0, veil);
        }
        renderFlash(g, h);
    }

    /** Handles the reset button and its confirmation popover first, then delegates to {@link #onMouseClicked}. */
    @Override
    public final boolean mouseClicked(double mx, double my, int button) {
        if (confirmOpen) {
            return confirmClicked(mx, my, button);
        }
        if (button == 0 && !hasOverlay() && resetShown() && overReset(mx, my)) {
            confirmOpen = true;
            confirmOpenedAt = Util.getMillis();
            playClick();
            return true;
        }
        return onMouseClicked(mx, my, button);
    }

    /** Click handling of the concrete widget; the reset button has already had its chance. */
    protected boolean onMouseClicked(double mx, double my, int button) {
        return false;
    }
}
