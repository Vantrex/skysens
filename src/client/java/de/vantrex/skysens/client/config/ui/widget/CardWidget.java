package de.vantrex.skysens.client.config.ui.widget;

import de.vantrex.skysens.client.config.ui.model.OptionNode;
import de.vantrex.skysens.client.config.ui.render.Animated;
import de.vantrex.skysens.client.config.ui.render.RenderUtil;
import de.vantrex.skysens.client.config.ui.render.Theme;
import de.vantrex.skysens.client.config.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;

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
 */
public abstract class CardWidget extends OptionWidget {

    /** Below this text column width the control is stacked under the text. */
    private static final int MIN_SIDE_TEXT_WIDTH = 60;
    private static final int STACK_GAP = 4;

    private final Animated hover = new Animated(0f, 120f);
    private List<FormattedCharSequence> descLines = List.of();
    private int cachedTextWidth = -1;
    private int cachedTextVersion = -1;
    private String cachedDescription;

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
        return width - controlWidth() - 3 * Theme.CARD_PADDING_X;
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
    }
}
