package de.vantrex.skysens.client.features.macro.editor.canvas;

import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.note.StickyNoteNode;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;

/** Draws a sticky note in world coordinates relative to its top left corner: a coloured note with wrapped text and a resize handle. */
public final class StickyNoteRenderer {

    /** Size of the resize handle in the bottom right corner, in world units. */
    public static final int HANDLE = 12;

    private static final int TEXT_COLOR = 0xFF2A2410;

    private StickyNoteRenderer() {
    }

    public static void render(GuiGraphicsExtractor g, NodeLayout layout, boolean selected, boolean hovered) {
        final MacroNode node = layout.node;
        final float alpha = node.disabled ? 0.4f : 1f;
        final int fill = (StickyNoteNode.colorOf(node).argb & 0xFFFFFF) | 0xE6000000;
        final int w = layout.w;
        final int h = layout.h;
        RenderUtil.shadow(g, 0, 0, w, h, 4);
        RenderUtil.roundedRect(g, 0, 0, w, h, 4, a(fill, alpha));
        // a darker band along the top, like the top edge of a note
        RenderUtil.roundedRect(g, 0, 0, w, 6, 4, a(0x30000000, alpha), true, true, false, false);

        final String text = Params.getString(node.params, "text", "");
        RenderUtil.scissor(g, 0, 0, w, h);
        int y = 10;
        final int lineH = UiText.lineHeight() + 2;
        outer:
        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.isEmpty()) {
                y += lineH;
                continue;
            }
            for (FormattedCharSequence line : UiText.wrap(paragraph, w - 16)) {
                if (y + UiText.lineHeight() > h - 4) {
                    break outer;
                }
                UiText.drawLine(g, line, 8, y, a(TEXT_COLOR, alpha));
                y += lineH;
            }
        }
        RenderUtil.unscissor(g);

        if (selected || hovered) {
            // resize handle: two diagonal strokes
            for (int d = 4; d <= 10; d += 3) {
                for (int k = 0; k <= d; k++) {
                    RenderUtil.rect(g, w - 3 - d + k, h - 3 - k, 1, 1, a(0x80000000, alpha));
                }
            }
        }
        if (selected || hovered) {
            RenderUtil.roundedRing(g, -1, -1, w + 2, h + 2, 5, selected ? Theme.ACCENT : Theme.TEXT_FAINT);
        }
    }

    private static int a(int argb, float alpha) {
        return alpha >= 1f ? argb : RenderUtil.withAlpha(argb, alpha);
    }

}
