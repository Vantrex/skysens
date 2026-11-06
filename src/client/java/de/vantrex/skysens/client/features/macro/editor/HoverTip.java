package de.vantrex.skysens.client.features.macro.editor;

import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.editor.field.FieldUi;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * One tooltip per frame: whoever hovers something calls {@code set(...)} while rendering, the editor screen draws it
 * last (above panels and popovers) and clears it.
 */
public final class HoverTip {

    private static final int MAX_W = 280;
    private static final int PAD = 6;

    private record Line(String text, int color) {
    }

    private static @Nullable List<Line> lines;
    private static int mouseX;
    private static int mouseY;

    private HoverTip() {
    }

    public static void set(String text, int mx, int my) {
        lines = List.of(new Line(text, Theme.TEXT));
        mouseX = mx;
        mouseY = my;
    }

    public static void setIssues(List<NodeIssue> issues, int mx, int my) {
        final List<Line> out = new ArrayList<>();
        for (NodeIssue issue : issues) {
            out.add(new Line(issue.message(), switch (issue.severity()) {
                case ERROR -> Theme.DANGER;
                case WARNING -> FieldUi.AMBER;
                case INFO -> Theme.TEXT_MUTED;
            }));
        }
        lines = out;
        mouseX = mx;
        mouseY = my;
    }

    public static void clear() {
        lines = null;
    }

    public static void render(GuiGraphicsExtractor g, int sw, int sh) {
        final List<Line> shown = lines;
        lines = null;
        if (shown == null || shown.isEmpty()) {
            return;
        }
        final List<List<FormattedCharSequence>> wrapped = new ArrayList<>();
        int width = 0;
        int height = 0;
        for (Line line : shown) {
            final List<FormattedCharSequence> parts = UiText.wrap(line.text(), MAX_W);
            wrapped.add(parts);
            width = Math.max(width, Math.min(MAX_W, UiText.width(line.text())));
            height += parts.size() * (UiText.lineHeight() + 1) + 3;
        }
        final int w = width + 2 * PAD + 10;
        final int h = height + 2 * PAD - 3;
        final int x = Math.max(4, Math.min(mouseX + 12, sw - w - 4));
        final int y = Math.max(4, Math.min(mouseY + 14, sh - h - 4));
        RenderUtil.shadow(g, x, y, w, h, 4);
        RenderUtil.roundedRectWithBorder(g, x, y, w, h, 4, Theme.POPOVER | 0xFF000000, Theme.POPOVER_BORDER);
        int cy = y + PAD;
        for (int i = 0; i < shown.size(); i++) {
            final boolean bullet = shown.size() > 1;
            if (bullet) {
                RenderUtil.circle(g, x + PAD + 2, cy + 4, 2, shown.get(i).color());
            }
            for (FormattedCharSequence part : wrapped.get(i)) {
                UiText.drawLine(g, part, x + PAD + (bullet ? 8 : 0), cy, shown.get(i).color() == Theme.TEXT ? Theme.TEXT : Theme.TEXT_MUTED);
                cy += UiText.lineHeight() + 1;
            }
            cy += 3;
        }
    }

}
