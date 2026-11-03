package de.vantrex.skysens.client.ui.widget;

import de.vantrex.skysens.client.ui.model.OptionKind;
import de.vantrex.skysens.client.ui.model.OptionNode;
import de.vantrex.skysens.client.ui.render.Animated;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * Collapsible section: a header card (name, description, animated chevron) that reveals nested child
 * widgets with an animated height. Expanded state is remembered across screen reopenings.
 */
public class AccordionWidget extends OptionWidget {

    private static final Set<String> EXPANDED = new HashSet<>();
    private static final int CHEVRON_SLOT = 14;
    private static final int BADGE_PAD = 5;
    private static final int BADGE_GAP = 6;
    private static final float DIM_ALPHA = 0.55f;

    private final List<OptionWidget> children = new ArrayList<>();
    private final String key;
    private final Animated progress;
    private final Animated hover = new Animated(0f, 120f);
    private boolean expanded;
    private boolean nested;

    private List<FormattedCharSequence> descLines = List.of();
    private int cachedTextVersion = -1;
    private int cachedTextWidth = -1;

    public AccordionWidget(OptionNode node, Function<OptionNode, OptionWidget> factory) {
        super(node);
        this.key = node.option().getPath();
        this.expanded = EXPANDED.contains(key);
        this.progress = new Animated(expanded ? 1f : 0f, 180f);
        for (OptionNode child : node.children()) {
            children.add(factory.apply(child));
        }
        applyNested();
    }

    /** Nested accordions use the nested background; their children alternate back to the normal one. */
    public void setNested(boolean nested) {
        this.nested = nested;
        applyNested();
    }

    private void applyNested() {
        for (OptionWidget c : children) {
            if (c instanceof CardWidget cw) {
                cw.setNested(!nested);
            } else if (c instanceof AccordionWidget aw) {
                aw.setNested(!nested);
            }
        }
    }

    /** Header badge text: On/Off for a master toggle, otherwise "x/y on" for two or more booleans, else null. */
    private String badgeText() {
        OptionNode master = null;
        int toggles = 0;
        int on = 0;
        for (OptionNode child : node.children()) {
            if (child.isMasterToggle()) {
                master = child;
            }
            if (child.kind() == OptionKind.TOGGLE) {
                toggles++;
                if (Boolean.TRUE.equals(child.get())) {
                    on++;
                }
            }
        }
        if (master != null) {
            return Boolean.TRUE.equals(master.get()) ? "On" : "Off";
        }
        return toggles >= 2 ? on + "/" + toggles + " on" : null;
    }

    private static int badgeWidth(String text) {
        return UiText.width(text, UiText.Weight.SEMIBOLD) + 2 * BADGE_PAD;
    }

    private int textWidth() {
        return Math.max(40, width - CHEVRON_SLOT - 3 * Theme.CARD_PADDING_X);
    }

    private List<FormattedCharSequence> descriptionLines() {
        int tw = textWidth();
        int ver = UiText.version();
        if (tw != cachedTextWidth || ver != cachedTextVersion) {
            cachedTextWidth = tw;
            cachedTextVersion = ver;
            String desc = node.description();
            descLines = desc.isEmpty() ? List.of() : UiText.wrap(desc, tw);
        }
        return descLines;
    }

    private int textBlockHeight() {
        int lh = UiText.lineHeight();
        int h = lh;
        List<FormattedCharSequence> lines = descriptionLines();
        if (!lines.isEmpty()) {
            h += 3 + lines.size() * (lh + 1) - 1;
        }
        return h;
    }

    private int headerHeight() {
        return Math.max(14, textBlockHeight()) + 2 * Theme.CARD_PADDING;
    }

    private static int bottomPad() {
        return Theme.style().cards() ? Theme.NESTED_INDENT - 2 : 0;
    }

    private int childWidth() {
        return Math.max(20, width - 2 * Theme.NESTED_INDENT);
    }

    private int fullBodyHeight() {
        int h = 0;
        for (OptionWidget c : children) {
            h += c.height() + Theme.CARD_GAP;
        }
        if (!children.isEmpty()) {
            h = h - Theme.CARD_GAP + bottomPad();
        }
        return h;
    }

    @Override
    public void layout(int x, int y, int width) {
        super.layout(x, y, width);
        int cy = y + headerHeight();
        int cx = x + Theme.NESTED_INDENT;
        int cw = childWidth();
        for (OptionWidget c : children) {
            c.layout(cx, cy, cw);
            cy += c.height() + Theme.CARD_GAP;
        }
    }

    @Override
    public int height() {
        float p = progress.value();
        int header = headerHeight();
        if (p <= 0.001f) {
            return header;
        }
        return header + Math.round(fullBodyHeight() * p);
    }

    private boolean bodyVisible() {
        return expanded || progress.value() > 0.001f;
    }

    private boolean inHeader(double mx, double my) {
        return RenderUtil.inside(mx, my, x, y, width, headerHeight());
    }

    private boolean inBody(double mx, double my) {
        int hh = headerHeight();
        return bodyVisible() && RenderUtil.inside(mx, my, x, y + hh, width, height() - hh);
    }

    private void toggle() {
        setExpanded(!expanded);
    }

    /** Opens or closes the section (animated) and remembers the state across screen openings. */
    public void setExpanded(boolean value) {
        if (expanded == value) {
            return;
        }
        expanded = value;
        progress.target(expanded ? 1f : 0f);
        if (expanded) {
            EXPANDED.add(key);
        } else {
            EXPANDED.remove(key);
            closeOverlay();
        }
    }

    /** Target state: true when open or opening. */
    public boolean isExpanded() {
        return expanded;
    }

    /** True while this accordion or a nested one is still animating its height. */
    public boolean isAnimating() {
        progress.value();
        if (progress.isAnimating()) {
            return true;
        }
        for (OptionWidget c : children) {
            if (c instanceof AccordionWidget a && a.isAnimating()) {
                return true;
            }
        }
        return false;
    }

    /** Child widgets in order (read-only view). */
    public List<OptionWidget> children() {
        return Collections.unmodifiableList(children);
    }

    @Override
    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        int h = height();
        int hh = headerHeight();
        hover.target(inHeader(mouseX, mouseY) ? 1f : 0f);
        int base = nested ? Theme.CARD_NESTED : Theme.CARD;
        var style = Theme.style();
        int hoverColour = RenderUtil.withAlpha(Theme.HOVER_OVERLAY, hover.value());
        if (!style.cards()) {
            // flat row: header tint, hairline under the header, guide line beside the indented children
            RenderUtil.rect(g, x, y, width, h, base);
            if ((hoverColour >>> 24) != 0) {
                RenderUtil.rect(g, x, y, width, hh, hoverColour);
            }
            RenderUtil.rect(g, x, y + hh - 1, width, 1, Theme.DIVIDER);
            if (h > hh) {
                RenderUtil.rect(g, x + Theme.NESTED_INDENT / 2, y + hh, 1, h - hh, Theme.DIVIDER);
            }
        } else {
            if (style.cardBorders()) {
                RenderUtil.roundedRectWithBorder(g, x, y, width, h, Theme.CARD_RADIUS, base, Theme.BORDER);
            } else {
                RenderUtil.roundedRect(g, x, y, width, h, Theme.CARD_RADIUS, base);
            }
            // hover tint only on the header strip
            if ((hoverColour >>> 24) != 0) {
                RenderUtil.roundedRect(g, x, y, width, hh, Theme.CARD_RADIUS, hoverColour,
                        true, true, h <= hh, h <= hh);
            }
        }

        int lh = UiText.lineHeight();
        int ty = y + (hh - textBlockHeight()) / 2;
        int tx = x + Theme.CARD_PADDING_X;
        String badge = badgeText();
        int titleWidth = badge == null ? textWidth() : textWidth() - badgeWidth(badge) - BADGE_GAP;
        UiText.draw(g, UiText.ellipsize(node.name(), titleWidth, UiText.Weight.SEMIBOLD), tx, ty, Theme.TEXT,
                UiText.Weight.SEMIBOLD);
        if (badge != null) {
            drawBadge(g, badge, x + width - Theme.CARD_PADDING_X - CHEVRON_SLOT - BADGE_GAP, ty - 1);
        }
        ty += lh + 3;
        for (FormattedCharSequence line : descriptionLines()) {
            UiText.drawLine(g, line, tx, ty, Theme.TEXT_MUTED);
            ty += lh + 1;
        }

        if (node.isDimmed()) {
            int veil = RenderUtil.withAlpha(base, DIM_ALPHA);
            RenderUtil.roundedRect(g, x, y, width, hh, style.cards() ? Theme.CARD_RADIUS : 0, veil,
                    true, true, h <= hh, h <= hh);
        }

        float p = progress.value();
        drawChevron(g, x + width - Theme.CARD_PADDING_X - CHEVRON_SLOT / 2, y + hh / 2, p);

        if (p > 0.001f || expanded) {
            RenderUtil.scissor(g, x, y + hh, width, Math.max(0, h - hh));
            for (OptionWidget c : children) {
                c.render(g, mouseX, mouseY, partialTick);
            }
            RenderUtil.unscissor(g);
        }
        renderFlash(g, hh);
    }

    /** Small pill ending at {@code rightX}: accent for "On" and counts, muted for "Off". */
    private static void drawBadge(GuiGraphicsExtractor g, String text, int rightX, int top) {
        boolean off = text.equals("Off");
        int w = badgeWidth(text);
        int h = UiText.lineHeight() + 2;
        int bx = rightX - w;
        RenderUtil.roundedRect(g, bx, top, w, h, h / 2, off ? Theme.TRACK : Theme.ACCENT_SOFT);
        UiText.draw(g, text, bx + BADGE_PAD, top + 2, off ? Theme.TEXT_MUTED : Theme.ACCENT,
                UiText.Weight.SEMIBOLD);
    }

    /** Chevron pointing right at p=0 and down at p=1, drawn with small fills. */
    private static void drawChevron(GuiGraphicsExtractor g, int cx, int cy, float p) {
        double angle = p * Math.PI / 2;
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        int colour = RenderUtil.lerpColor(p, Theme.TEXT_MUTED, Theme.TEXT);
        // arms of a right-pointing chevron: tip at (1.5, 0), ends at (-1.5, -3.5) and (-1.5, 3.5)
        double tipX = 1.5;
        for (int s = -1; s <= 1; s += 2) {
            for (int i = 0; i <= 7; i++) {
                double t = i / 7.0;
                double px = tipX - 3.0 * t;
                double py = s * 3.5 * t;
                double rx = px * cos - py * sin;
                double ry = px * sin + py * cos;
                int ix = cx + (int) Math.round(rx - 0.5);
                int iy = cy + (int) Math.round(ry - 0.5);
                g.fill(ix, iy, ix + 1, iy + 1, colour);
                g.fill(ix + 1, iy, ix + 2, iy + 1, colour);
            }
        }
    }

    // ------------------------------------------------------------------ overlays

    @Override
    public void renderOverlay(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
        for (OptionWidget c : children) {
            c.renderOverlay(g, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean hasOverlay() {
        for (OptionWidget c : children) {
            if (c.hasOverlay()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean overlayContains(double mx, double my) {
        for (OptionWidget c : children) {
            if (c.hasOverlay() && c.overlayContains(mx, my)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void notifyReset() {
        for (OptionWidget c : children) {
            c.notifyReset();
        }
    }

    @Override
    public void closeOverlay() {
        for (OptionWidget c : children) {
            c.closeOverlay();
        }
    }

    // ------------------------------------------------------------------ events

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        for (OptionWidget c : children) {
            if (c.hasOverlay() && c.overlayContains(mx, my)) {
                return c.mouseClicked(mx, my, button) | true;
            }
        }
        if (inHeader(mx, my)) {
            if (button == 0) {
                playClick();
                toggle();
            }
            return true;
        }
        if (inBody(mx, my)) {
            for (OptionWidget c : children) {
                if (c.mouseClicked(mx, my, button)) {
                    return true;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (!bodyVisible()) {
            return false;
        }
        boolean used = false;
        for (OptionWidget c : children) {
            used |= c.mouseReleased(mx, my, button);
        }
        return used;
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (!bodyVisible()) {
            return false;
        }
        for (OptionWidget c : children) {
            if (c.mouseDragged(mx, my, button, dx, dy)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double amount) {
        for (OptionWidget c : children) {
            if (c.hasOverlay() && c.overlayContains(mx, my)) {
                return c.mouseScrolled(mx, my, amount);
            }
        }
        if (!inBody(mx, my)) {
            return false;
        }
        for (OptionWidget c : children) {
            if (c.mouseScrolled(mx, my, amount)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (!bodyVisible()) {
            return false;
        }
        for (OptionWidget c : children) {
            if (c.keyPressed(e)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (!bodyVisible()) {
            return false;
        }
        for (OptionWidget c : children) {
            if (c.charTyped(e)) {
                return true;
            }
        }
        return false;
    }
}
