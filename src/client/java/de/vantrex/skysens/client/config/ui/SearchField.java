package de.vantrex.skysens.client.config.ui;

import de.vantrex.skysens.client.config.ui.render.RenderUtil;
import de.vantrex.skysens.client.config.ui.render.Theme;
import de.vantrex.skysens.client.config.ui.render.UiText;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;

import java.util.function.Consumer;

/** Small self-drawn single-line text input used for the settings search. */
public final class SearchField {

    private static final int HINT_PAD = 4;

    private int x, y, w, h;
    private String text = "";
    private int caret;
    private boolean focused;
    private boolean selectAll;
    private long focusMillis;
    private final Consumer<String> onChange;

    public SearchField(Consumer<String> onChange) {
        this.onChange = onChange;
    }

    public void layout(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public String text() {
        return text;
    }

    public boolean isFocused() {
        return focused;
    }

    public void setFocused(boolean focused) {
        if (focused && !this.focused) {
            focusMillis = Util.getMillis();
        }
        this.focused = focused;
        if (!focused) {
            selectAll = false;
        }
    }

    public void setText(String t) {
        this.text = t == null ? "" : t;
        this.caret = this.text.length();
        this.selectAll = false;
        onChange.accept(this.text);
    }

    public boolean isOver(double mx, double my) {
        return RenderUtil.inside(mx, my, x, y, w, h);
    }

    public void render(GuiGraphicsExtractor g, int mouseX, int mouseY) {
        int lh = UiText.lineHeight();
        RenderUtil.inputBox(g, x, y, w, h, 4, focused, Theme.ACCENT, Theme.BORDER);

        int ty = y + (h - lh) / 2 + 1;
        int tx = x + 6;
        // "Ctrl F" hint chip on the right
        String hint = "Ctrl F";
        int chipW = UiText.width(hint) + HINT_PAD * 2;
        int chipH = lh + 2;
        int chipX = x + w - chipW - 3;
        int chipY = y + (h - chipH) / 2;
        String placeholder = "Search settings…";
        // only show the hint chip when the placeholder still fits next to it
        boolean showChip = text.isEmpty() && !focused && chipX - 4 - tx >= UiText.width(placeholder);
        if (showChip) {
            RenderUtil.roundedRect(g, chipX, chipY, chipW, chipH, 3, Theme.BORDER);
            UiText.draw(g, hint, chipX + HINT_PAD, chipY + 2, Theme.TEXT_FAINT);
        }
        int maxW = (showChip ? chipX - 4 : x + w - 6) - tx;

        // Scroll the text so the caret stays visible
        String before = text.substring(0, caret);
        int caretPx = UiText.width(before);
        int shift = Math.max(0, caretPx - maxW + 2);
        RenderUtil.scissor(g, tx, y + 1, Math.max(0, maxW), h - 2);
        if (text.isEmpty()) {
            UiText.draw(g, placeholder, tx, ty, Theme.TEXT_FAINT);
        }
        if (selectAll && focused && !text.isEmpty()) {
            RenderUtil.rect(g, tx - shift, ty - 1, UiText.width(text), lh + 1, Theme.ACCENT_SOFT);
        }
        if (!text.isEmpty()) {
            UiText.draw(g, text, tx - shift, ty, Theme.TEXT);
        }
        if (focused && ((Util.getMillis() - focusMillis) / 500) % 2 == 0) {
            RenderUtil.rect(g, tx - shift + caretPx, ty - 1, 1, lh + 1, Theme.TEXT);
        }
        RenderUtil.unscissor(g);
    }

    public boolean mouseClicked(double mx, double my) {
        boolean over = isOver(mx, my);
        setFocused(over);
        if (over) {
            caret = text.length();
            selectAll = false;
            focusMillis = Util.getMillis();
        }
        return over;
    }

    public boolean keyPressed(KeyEvent e) {
        if (!focused) {
            return false;
        }
        if (e.isSelectAll()) {
            selectAll = !text.isEmpty();
            caret = text.length();
            return true;
        }
        if (e.isPaste()) {
            String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null && !clip.isEmpty()) {
                insert(sanitize(clip));
            }
            return true;
        }
        if (e.isCopy()) {
            return true;
        }
        // same modifier helper as vanilla EditBox (Cmd on macOS, Ctrl elsewhere)
        boolean word = e.hasControlDownWithQuirk();
        switch (e.key()) {
            case InputConstants.KEY_BACKSPACE -> {
                if (selectAll) {
                    replaceAll("");
                } else if (word) {
                    deleteRange(wordPosition(-1), caret);
                } else if (caret > 0) {
                    text = text.substring(0, caret - 1) + text.substring(caret);
                    caret--;
                    changed();
                }
                return true;
            }
            case InputConstants.KEY_DELETE -> {
                if (selectAll) {
                    replaceAll("");
                } else if (word) {
                    deleteRange(caret, wordPosition(1));
                } else if (caret < text.length()) {
                    text = text.substring(0, caret) + text.substring(caret + 1);
                    changed();
                }
                return true;
            }
            case InputConstants.KEY_LEFT -> {
                selectAll = false;
                caret = word ? wordPosition(-1) : Math.max(0, caret - 1);
                focusMillis = Util.getMillis();
                return true;
            }
            case InputConstants.KEY_RIGHT -> {
                selectAll = false;
                caret = word ? wordPosition(1) : Math.min(text.length(), caret + 1);
                focusMillis = Util.getMillis();
                return true;
            }
            case InputConstants.KEY_HOME -> {
                selectAll = false;
                caret = 0;
                return true;
            }
            case InputConstants.KEY_END -> {
                selectAll = false;
                caret = text.length();
                return true;
            }
            default -> {
                // swallow plain typing keys so they do not trigger other handlers
                return !e.hasControlDown();
            }
        }
    }

    public boolean charTyped(CharacterEvent e) {
        if (!focused || !e.isAllowedChatCharacter()) {
            return false;
        }
        insert(e.codepointAsString());
        return true;
    }

    private static String sanitize(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 32 && c != 127) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    private void insert(String s) {
        // check the length before clearing a selection, otherwise an over-long paste would empty the field
        // without notifying the listener (results would still show the old query)
        if ((selectAll ? 0 : text.length()) + s.length() > 64) {
            return;
        }
        if (selectAll) {
            text = "";
            caret = 0;
            selectAll = false;
        }
        if (text.length() + s.length() > 64) {
            return;
        }
        text = text.substring(0, caret) + s + text.substring(caret);
        caret += s.length();
        changed();
    }

    /**
     * Word boundary from the caret, mirroring vanilla {@code EditBox.getWordPosition}: backwards skips spaces
     * then the word; forwards runs to the end of the current word then skips the spaces after it.
     */
    private int wordPosition(int direction) {
        int pos = caret;
        int len = text.length();
        if (direction < 0) {
            while (pos > 0 && text.charAt(pos - 1) == ' ') {
                pos--;
            }
            while (pos > 0 && text.charAt(pos - 1) != ' ') {
                pos--;
            }
        } else {
            int space = text.indexOf(' ', pos);
            pos = space == -1 ? len : space;
            while (pos < len && text.charAt(pos) == ' ') {
                pos++;
            }
        }
        return pos;
    }

    /** Removes text[from, to) (any order) and puts the caret at the start of the removed range. */
    private void deleteRange(int from, int to) {
        int a = Math.min(from, to);
        int b = Math.max(from, to);
        if (a == b) {
            return;
        }
        text = text.substring(0, a) + text.substring(b);
        caret = a;
        changed();
    }

    private void replaceAll(String s) {
        text = s;
        caret = s.length();
        selectAll = false;
        changed();
    }

    private void changed() {
        focusMillis = Util.getMillis();
        onChange.accept(text);
    }
}
