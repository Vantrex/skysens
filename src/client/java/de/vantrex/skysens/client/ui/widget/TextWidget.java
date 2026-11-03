package de.vantrex.skysens.client.ui.widget;

import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.ui.model.OptionNode;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;

/**
 * String option ({@code @ConfigEditorText}) rendered as a single-line input box. Minimal editing: typing and
 * pasting append at the end, backspace removes the last character (Ctrl+Backspace clears), Enter unfocuses.
 * Edits are written to the option live. Only one text box is focused at a time.
 */
public class TextWidget extends CardWidget {

    private static final int BOX_W = 170;
    private static final int BOX_H = 18;
    private static final int MAX_LENGTH = 512;

    private static TextWidget focusedWidget;

    public TextWidget(OptionNode node) {
        super(node);
    }

    private String text() {
        return node.get() instanceof String s ? s : "";
    }

    private boolean focused() {
        return focusedWidget == this;
    }

    private void write(String value) {
        if (!value.equals(text())) {
            node.set(value);
        }
    }

    @Override
    protected int controlWidth() {
        return BOX_W;
    }

    @Override
    protected int controlHeight() {
        return BOX_H;
    }

    @Override
    protected void renderControl(GuiGraphicsExtractor g, int cx, int cy, int mouseX, int mouseY,
                                 float partialTick) {
        boolean focused = focused();
        RenderUtil.inputBox(g, cx, cy, BOX_W, BOX_H, 4, focused, Theme.ACCENT, Theme.BORDER);
        String text = text();
        int lh = UiText.lineHeight();
        int tx = cx + 6;
        int ty = cy + (BOX_H - lh) / 2 + 1;
        int maxW = BOX_W - 12;
        int shift = Math.max(0, UiText.width(text) - maxW + 2);
        RenderUtil.scissor(g, tx, cy + 1, maxW, BOX_H - 2);
        if (text.isEmpty()) {
            UiText.draw(g, "comma separated", tx, ty, Theme.TEXT_FAINT);
        } else {
            UiText.draw(g, text, tx - shift, ty, Theme.TEXT);
        }
        if (focused && (Util.getMillis() / 500) % 2 == 0) {
            RenderUtil.rect(g, tx - shift + UiText.width(text), ty - 1, 1, lh + 1, Theme.TEXT);
        }
        RenderUtil.unscissor(g);
    }

    @Override
    protected boolean onMouseClicked(double mx, double my, int button) {
        if (button == 0 && RenderUtil.inside(mx, my, controlX(), controlY(), BOX_W, BOX_H)) {
            focusedWidget = this;
            return true;
        }
        if (focused()) {
            focusedWidget = null;
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (!focused()) {
            return false;
        }
        if (e.isPaste()) {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null) {
                append(clip);
            }
            return true;
        }
        switch (e.key()) {
            case InputConstants.KEY_BACKSPACE -> {
                String text = text();
                if (e.hasControlDownWithQuirk()) {
                    write("");
                } else if (!text.isEmpty()) {
                    write(text.substring(0, text.length() - 1));
                }
                return true;
            }
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> {
                focusedWidget = null;
                return true;
            }
            default -> {
                return !e.hasControlDown();
            }
        }
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (!focused() || !e.isAllowedChatCharacter()) {
            return false;
        }
        append(e.codepointAsString());
        return true;
    }

    private void append(String s) {
        String clean = s.replaceAll("[\\p{Cntrl}]", "");
        String text = text();
        if (text.length() + clean.length() <= MAX_LENGTH) {
            write(text + clean);
        }
    }
}
