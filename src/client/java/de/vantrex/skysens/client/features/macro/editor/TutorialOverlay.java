package de.vantrex.skysens.client.features.macro.editor;

import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;
import java.util.function.Function;

/**
 * The first-run tour: dims the screen, outlines the part of the UI a step is about and shows a card with a short text
 * and Back / Next / Skip. The steps are a static list (anchor + text), so adding one is one line. 
 */
public final class TutorialOverlay {

    /** The part of the UI a step highlights. The screen resolves it to a rectangle {x, y, w, h}. */
    public enum Anchor { LIST, ADD_BUTTON, CANVAS, DETAILS, LOGIC_NODE, EXECUTIONS_TAB }

    public record Step(Anchor anchor, String title, String text) {
    }

    public static final List<Step> STEPS = List.of(
            new Step(Anchor.LIST, "Macro list",
                    "Create a macro and switch it on or off with its knob. Macros higher in the list win when they share a key."),
            new Step(Anchor.ADD_BUTTON, "Add a node",
                    "The + button or Tab opens the node picker. Every macro starts with a Hotkey Trigger."),
            new Step(Anchor.CANVAS, "Connect",
                    "Drag from an output dot to an input dot to connect two nodes. Delete removes a wire or node."),
            new Step(Anchor.DETAILS, "Configure",
                    "Double-click a node to open its settings panel. It holds the conditions, the command text and placeholders like {{ island }}."),
            new Step(Anchor.LOGIC_NODE, "If and Switch",
                    "If has a true and a false output. In a Switch, the first matching rule wins. \"Unknown\" (for example right after joining) never matches."),
            new Step(Anchor.CANVAS, "Navigate",
                    "Drag on empty space to select. Pan with the middle mouse button, Space + drag or the scroll wheel, zoom with Ctrl + wheel. Ctrl+C / Ctrl+V copy and paste nodes, Ctrl+Z / Ctrl+Y undo and redo, D disables a node. Ctrl+S saves right away (closing the editor saves too). Press F1 or \"Keys\" any time for the full shortcut list."),
            new Step(Anchor.EXECUTIONS_TAB, "Test",
                    "Press the key in game and the path it took lights up green. The Executions tab shows why something did or didn't run.")
    );

    private static final int CARD_W = 240;
    private static final int PAD = 10;
    private static final int BUTTON_H = 18;
    private static final int DIM = 0xA0000000;

    private final Function<Anchor, int[]> anchors;
    private final Runnable onFinished;

    private boolean open;
    private int index;
    private int[] nextButton = new int[4];
    private int[] backButton = new int[4];
    private int[] skipButton = new int[4];
    private boolean hasBack;

    public TutorialOverlay(Function<Anchor, int[]> anchors, Runnable onFinished) {
        this.anchors = anchors;
        this.onFinished = onFinished;
    }

    public boolean isOpen() {
        return this.open;
    }

    public void open() {
        this.open = true;
        this.index = 0;
    }

    /** Closes the tour (finished or skipped) and tells the owner so it can remember that it was seen. */
    public void finish() {
        if (this.open) {
            this.open = false;
            this.onFinished.run();
        }
    }

    private void next() {
        if (this.index >= STEPS.size() - 1) {
            this.finish();
        } else {
            this.index++;
        }
    }

    private void back() {
        this.index = Math.max(0, this.index - 1);
    }

    public void render(GuiGraphicsExtractor g, int mx, int my, int sw, int sh) {
        if (!this.open) {
            return;
        }
        final Step step = STEPS.get(this.index);
        final int[] target = this.anchors.apply(step.anchor());
        final int pad = 4;
        final int tx = Math.max(0, target[0] - pad);
        final int ty = Math.max(0, target[1] - pad);
        final int tw = Math.min(sw - tx, target[2] + 2 * pad);
        final int th = Math.min(sh - ty, target[3] + 2 * pad);

        // dim everything but the target
        RenderUtil.rect(g, 0, 0, sw, ty, DIM);
        RenderUtil.rect(g, 0, ty + th, sw, sh - ty - th, DIM);
        RenderUtil.rect(g, 0, ty, tx, th, DIM);
        RenderUtil.rect(g, tx + tw, ty, sw - tx - tw, th, DIM);
        RenderUtil.roundedRing(g, tx, ty, tw, th, 6, Theme.ACCENT);
        RenderUtil.roundedRing(g, tx - 1, ty - 1, tw + 2, th + 2, 7, RenderUtil.withAlpha(Theme.ACCENT, 0.4f));

        // card
        final List<FormattedCharSequence> lines = UiText.wrap(step.text(), CARD_W - 2 * PAD);
        final int lineH = UiText.lineHeight() + 2;
        final int cardH = PAD + 12 + 6 + lines.size() * lineH + 10 + BUTTON_H + PAD;
        int cx;
        int cy;
        if (tx + tw + 12 + CARD_W <= sw) {
            cx = tx + tw + 12;
            cy = ty + th / 2 - cardH / 2;
        } else if (tx - 12 - CARD_W >= 0) {
            cx = tx - 12 - CARD_W;
            cy = ty + th / 2 - cardH / 2;
        } else if (ty + th + 12 + cardH <= sh) {
            cx = tx + tw / 2 - CARD_W / 2;
            cy = ty + th + 12;
        } else if (ty - 12 - cardH >= 0) {
            cx = tx + tw / 2 - CARD_W / 2;
            cy = ty - 12 - cardH;
        } else {
            cx = sw / 2 - CARD_W / 2;
            cy = sh / 2 - cardH / 2;
        }
        cx = Math.max(8, Math.min(cx, sw - CARD_W - 8));
        cy = Math.max(8, Math.min(cy, sh - cardH - 8));

        RenderUtil.shadow(g, cx, cy, CARD_W, cardH, 6);
        RenderUtil.roundedRectWithBorder(g, cx, cy, CARD_W, cardH, 6, Theme.POPOVER | 0xFF000000, Theme.POPOVER_BORDER);
        UiText.draw(g, step.title(), cx + PAD, cy + PAD, Theme.TEXT, UiText.Weight.SEMIBOLD);
        UiText.drawRight(g, (this.index + 1) + " / " + STEPS.size(), cx + CARD_W - PAD, cy + PAD, Theme.TEXT_FAINT);
        int ly = cy + PAD + 12 + 6;
        for (FormattedCharSequence line : lines) {
            UiText.drawLine(g, line, cx + PAD, ly, Theme.TEXT_MUTED);
            ly += lineH;
        }

        final int by = cy + cardH - PAD - BUTTON_H;
        final boolean last = this.index == STEPS.size() - 1;
        final String nextLabel = last ? "Done" : "Next";
        final int nextW = UiText.width(nextLabel) + 24;
        this.nextButton = new int[]{cx + CARD_W - PAD - nextW, by, nextW, BUTTON_H};
        this.hasBack = this.index > 0;
        final int backW = UiText.width("Back") + 24;
        this.backButton = new int[]{this.nextButton[0] - 6 - backW, by, backW, BUTTON_H};
        final int skipW = UiText.width("Skip") + 12;
        this.skipButton = new int[]{cx + PAD, by, skipW, BUTTON_H};

        button(g, this.nextButton, nextLabel, true, mx, my);
        if (this.hasBack) {
            button(g, this.backButton, "Back", false, mx, my);
        }
        if (!last) {
            final boolean hover = inside(this.skipButton, mx, my);
            UiText.drawCentered(g, "Skip", this.skipButton[0] + skipW / 2, by + (BUTTON_H - 9) / 2, hover ? Theme.TEXT : Theme.TEXT_FAINT);
        }
    }

    private static void button(GuiGraphicsExtractor g, int[] r, String label, boolean primary, int mx, int my) {
        final boolean hover = inside(r, mx, my);
        final int fill = primary ? (hover ? Theme.ACCENT_HOVER : Theme.ACCENT) : (hover ? Theme.CARD_HOVER : Theme.CARD);
        RenderUtil.roundedRect(g, r[0], r[1], r[2], r[3], 5, fill);
        UiText.drawCentered(g, label, r[0] + r[2] / 2, r[1] + (r[3] - 9) / 2, primary ? 0xFFFFFFFF : Theme.TEXT);
    }

    private static boolean inside(int[] r, double mx, double my) {
        return RenderUtil.inside(mx, my, r[0], r[1], r[2], r[3]);
    }

    /** Modal: always consumes the click while the tour is open. */
    public boolean mouseClicked(double mx, double my, int button) {
        if (!this.open) {
            return false;
        }
        if (button == 0) {
            if (inside(this.nextButton, mx, my)) {
                this.next();
            } else if (this.hasBack && inside(this.backButton, mx, my)) {
                this.back();
            } else if (this.index < STEPS.size() - 1 && inside(this.skipButton, mx, my)) {
                this.finish();
            }
        }
        return true;
    }

    public boolean keyPressed(int key) {
        if (!this.open) {
            return false;
        }
        switch (key) {
            case InputConstants.KEY_ESCAPE -> this.finish();
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER, InputConstants.KEY_RIGHT -> this.next();
            case InputConstants.KEY_LEFT -> this.back();
            default -> {
            }
        }
        return true;
    }

}
