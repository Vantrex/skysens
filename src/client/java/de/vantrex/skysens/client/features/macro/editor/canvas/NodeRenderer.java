package de.vantrex.skysens.client.features.macro.editor.canvas;

import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.editor.Highlight;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.NodeCategory;
import de.vantrex.skysens.client.features.macro.node.Port;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Draws one node card in world coordinates (the caller has set up the canvas transform). Card with a coloured icon
 * block on the left (the category colour), title and summary, an input port on the left and output ports on the
 * right; If / Switch labels sit inside the card next to their port.
 */
public final class NodeRenderer {

    public static final int GREEN = 0xFF3FB950;
    public static final int AMBER = 0xFFE3A52B;

    public static final int BADGE_RADIUS = 7;
    public static final int BADGE_INSET = 4;
    private static final int ICON_WIDTH = 28;
    private static final int RADIUS = 6;
    private static final int LABEL_MAX = 56;
    private static final float DISABLED_ALPHA = 0.4f;

    private NodeRenderer() {
    }

    /** Centre of the issue badge relative to the node origin. */
    public static int[] badgeCenter(NodeLayout layout) {
        return new int[]{layout.w - BADGE_INSET, BADGE_INSET - 2};
    }

    public static int categoryColor(NodeCategory category) {
        return switch (category) {
            case TRIGGER -> 0xFF2F9E6B;
            case LOGIC -> 0xFFE08A2E;
            case CONDITION -> 0xFF8E5BD0;
            case ACTION -> 0xFF4A7BD6;
            case NOTE -> 0xFFE8C547;
            case UNKNOWN -> 0xFF9B3B3B;
        };
    }

    public static int highlightColor(Highlight.Kind kind) {
        return kind == Highlight.Kind.CHOSEN ? GREEN : AMBER;
    }

    /**
     * @param outputsConnected ids of the output ports that have a wire
     */
    public static void render(GuiGraphicsExtractor g, NodeLayout layout, boolean selected, boolean hovered,
                              Highlight.Kind highlight, float highlightStrength, boolean inputConnected,
                              Set<String> outputsConnected, NodeIssue.@Nullable Severity issue) {
        final MacroNode node = layout.node;
        g.pose().pushMatrix();
        g.pose().translate(node.x, node.y);
        if (layout.sticky) {
            StickyNoteRenderer.render(g, layout, selected, hovered);
        } else {
            renderCard(g, layout, selected, hovered, highlight, highlightStrength, inputConnected, outputsConnected, issue);
        }
        g.pose().popMatrix();
    }

    private static void renderCard(GuiGraphicsExtractor g, NodeLayout layout, boolean selected, boolean hovered,
                                   Highlight.Kind highlight, float strength, boolean inputConnected, Set<String> outputsConnected,
                                   NodeIssue.@Nullable Severity issue) {
        final MacroNode node = layout.node;
        final float alpha = node.disabled ? DISABLED_ALPHA : 1f;
        final int w = layout.w;
        final int h = layout.h;

        if (!node.disabled) {
            RenderUtil.shadow(g, 0, 0, w, h, RADIUS);
        }
        RenderUtil.roundedRectWithBorder(g, 0, 0, w, h, RADIUS, a(Theme.POPOVER | 0xFF000000, alpha),
                a(hovered ? Theme.TEXT_FAINT : Theme.POPOVER_BORDER, alpha));

        // coloured icon block
        RenderUtil.roundedRect(g, 1, 1, ICON_WIDTH, h - 2, RADIUS - 1, a(categoryColor(layout.type.category()), alpha),
                true, false, true, false);
        UiText.drawCentered(g, layout.type.icon(), 1 + ICON_WIDTH / 2, h / 2 - 4, a(0xFFFFFFFF, alpha), UiText.Weight.SEMIBOLD);

        // title and summary
        int labelColumn = 0;
        if (layout.labelled) {
            for (Port port : layout.outputs) {
                labelColumn = Math.max(labelColumn, Math.min(LABEL_MAX, UiText.width(port.label())));
            }
            labelColumn += 8;
        }
        final int textX = ICON_WIDTH + 8;
        final int textW = Math.max(10, w - textX - 8 - labelColumn);
        final int top = h > 44 ? 10 : 11;
        final String title = node.name != null && !node.name.isBlank() ? node.name : layout.type.displayName();
        final String shownTitle = UiText.ellipsize(title, textW, UiText.Weight.SEMIBOLD);
        UiText.draw(g, shownTitle, textX, top, a(Theme.TEXT, alpha), UiText.Weight.SEMIBOLD);
        UiText.draw(g, UiText.ellipsize(layout.type.summary(node), textW), textX, top + 12, a(Theme.TEXT_MUTED, alpha));
        if (node.disabled) {
            RenderUtil.rect(g, textX, top + 4, UiText.width(shownTitle, UiText.Weight.SEMIBOLD), 1, a(Theme.TEXT, alpha));
        }

        // output labels, right aligned inside the card
        if (layout.labelled) {
            for (int i = 0; i < layout.outputs.size(); i++) {
                final String label = UiText.ellipsize(layout.outputs.get(i).label(), LABEL_MAX);
                UiText.drawRight(g, label, w - 10, layout.outputY(i) - 4, a(Theme.TEXT_MUTED, alpha));
            }
        }

        // ports
        if (!layout.inputs.isEmpty()) {
            port(g, 0, layout.inputY(), inputConnected, alpha);
        }
        for (int i = 0; i < layout.outputs.size(); i++) {
            port(g, w, layout.outputY(i), outputsConnected.contains(layout.outputs.get(i).id()), alpha);
        }

        // outlines: execution highlight, then selection
        if (highlight != null && strength > 0f) {
            final int color = highlightColor(highlight);
            RenderUtil.roundedRing(g, -3, -3, w + 6, h + 6, RADIUS + 3, RenderUtil.withAlpha(color, 0.25f * strength));
            RenderUtil.roundedRing(g, -2, -2, w + 4, h + 4, RADIUS + 2, RenderUtil.withAlpha(color, 0.6f * strength + 0.2f));
            RenderUtil.roundedRing(g, -1, -1, w + 2, h + 2, RADIUS + 1, RenderUtil.withAlpha(color, strength));
        }
        if (selected) {
            RenderUtil.roundedRing(g, -2, -2, w + 4, h + 4, RADIUS + 2, Theme.ACCENT);
            RenderUtil.roundedRing(g, -1, -1, w + 2, h + 2, RADIUS + 1, Theme.ACCENT);
        }
        if (issue != null) {
            final int bx = w - BADGE_INSET;
            final int by = BADGE_INSET - 2;
            RenderUtil.circle(g, bx, by, BADGE_RADIUS + 1, Theme.POPOVER | 0xFF000000);
            RenderUtil.circle(g, bx, by, BADGE_RADIUS, issue == NodeIssue.Severity.ERROR ? Theme.DANGER : AMBER);
            UiText.drawCentered(g, "!", bx, by - 4, 0xFFFFFFFF, UiText.Weight.SEMIBOLD);
        }
    }

    private static void port(GuiGraphicsExtractor g, int cx, int cy, boolean connected, float alpha) {
        RenderUtil.circle(g, cx, cy, NodeLayout.PORT_RADIUS + 1, a(Theme.POPOVER_BORDER | 0xFF000000, alpha));
        RenderUtil.circle(g, cx, cy, NodeLayout.PORT_RADIUS, a(connected ? Theme.TEXT_MUTED : Theme.TEXT_FAINT, alpha));
    }

    private static int a(int argb, float alpha) {
        return alpha >= 1f ? argb : RenderUtil.withAlpha(argb, alpha);
    }

}
