package de.vantrex.skysens.client.features.macro.editor.field;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * TEXT_MULTILINE parameter (sticky notes): a word-wrapping text area. Enter inserts a line break; arrows, Home, End,
 * Backspace, Delete and paste work, there is no selection.
 */
public final class MultiLineParamField extends ParamField {

    private static final int MAX_LENGTH = 600;
    private static final int MIN_LINES = 4;
    private static final int MAX_LINES = 10;
    private static final int PAD = 6;

    private record Line(int start, int end) {
    }

    private String text;
    private int caret;
    private boolean focused;
    private long focusMillis;
    private int scrollLine;

    private List<Line> lines = List.of(new Line(0, 0));
    private String linesFor;
    private int linesWidth = -1;

    private int x;
    private int y;
    private int w;
    private int boxY;
    private int boxH;

    public MultiLineParamField(ParamSpec spec, JsonObject params, FieldContext ctx) {
        super(spec, params, ctx);
        this.text = Params.getString(params, spec.key(), "");
        this.caret = this.text.length();
    }

    private void wrap(int innerW) {
        if (this.text.equals(this.linesFor) && innerW == this.linesWidth) {
            return;
        }
        final List<Line> out = new ArrayList<>();
        int paragraph = 0;
        while (true) {
            int newline = this.text.indexOf('\n', paragraph);
            final int paragraphEnd = newline < 0 ? this.text.length() : newline;
            int start = paragraph;
            while (true) {
                int end = start;
                int lastSpace = -1;
                while (end < paragraphEnd && UiText.width(this.text.substring(start, end + 1)) <= innerW) {
                    if (this.text.charAt(end) == ' ') {
                        lastSpace = end;
                    }
                    end++;
                }
                if (end < paragraphEnd) {
                    // did not fit: break after the last space, or inside the word
                    final int cut = lastSpace > start ? lastSpace + 1 : Math.max(end, start + 1);
                    out.add(new Line(start, cut));
                    start = cut;
                } else {
                    out.add(new Line(start, paragraphEnd));
                    break;
                }
            }
            if (newline < 0) {
                break;
            }
            paragraph = newline + 1;
        }
        this.lines = out;
        this.linesFor = this.text;
        this.linesWidth = innerW;
    }

    private int visibleLines() {
        return Math.max(MIN_LINES, Math.min(MAX_LINES, this.lines.size()));
    }

    private int lineOfCaret() {
        int found = 0;
        for (int i = 0; i < this.lines.size(); i++) {
            if (this.lines.get(i).start() <= this.caret) {
                found = i;
            }
        }
        return found;
    }

    @Override
    protected java.util.List<net.minecraft.util.FormattedCharSequence> descriptionLines(int w) {
        return List.of();
    }

    @Override
    public int height(int w) {
        this.wrap(w - 2 * PAD);
        return LABEL_H + this.visibleLines() * (UiText.lineHeight() + 1) + 2 * PAD - 2;
    }

    @Override
    public void render(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.wrap(w - 2 * PAD);
        FieldUi.label(g, this.spec.label(), x, y);
        final int lh = UiText.lineHeight() + 1;
        this.boxY = y + LABEL_H;
        this.boxH = this.visibleLines() * lh + 2 * PAD - 2;
        RenderUtil.inputBox(g, x, this.boxY, w, this.boxH, 4, this.focused, Theme.ACCENT, Theme.BORDER);

        final int visible = this.visibleLines();
        final int caretLine = this.lineOfCaret();
        if (caretLine < this.scrollLine) {
            this.scrollLine = caretLine;
        } else if (caretLine >= this.scrollLine + visible) {
            this.scrollLine = caretLine - visible + 1;
        }
        this.scrollLine = Math.max(0, Math.min(this.scrollLine, Math.max(0, this.lines.size() - visible)));

        RenderUtil.scissor(g, x + 1, this.boxY + 1, w - 2, this.boxH - 2);
        if (this.text.isEmpty()) {
            UiText.draw(g, this.spec.description() == null ? "" : this.spec.description(), x + PAD, this.boxY + PAD - 1, Theme.TEXT_FAINT);
        }
        for (int i = this.scrollLine; i < Math.min(this.lines.size(), this.scrollLine + visible); i++) {
            final Line line = this.lines.get(i);
            final int ly = this.boxY + PAD - 1 + (i - this.scrollLine) * lh;
            final String shown = this.text.substring(line.start(), line.end());
            if (!shown.isEmpty()) {
                UiText.draw(g, shown, x + PAD, ly, Theme.TEXT);
            }
            if (this.focused && i == caretLine && ((Util.getMillis() - this.focusMillis) / 500) % 2 == 0) {
                final int cx = x + PAD + UiText.width(this.text.substring(line.start(), Math.min(this.caret, line.end())));
                RenderUtil.rect(g, cx, ly - 1, 1, UiText.lineHeight() + 1, Theme.TEXT);
            }
        }
        RenderUtil.unscissor(g);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        final boolean over = button == 0 && RenderUtil.inside(mx, my, this.x, this.boxY, this.w, this.boxH);
        this.focused = over;
        if (!over) {
            return false;
        }
        final int lh = UiText.lineHeight() + 1;
        final int row = (int) Math.max(0, Math.min(this.lines.size() - this.scrollLine - 1, (my - this.boxY - PAD + 1) / lh));
        final Line line = this.lines.get(Math.min(this.lines.size() - 1, this.scrollLine + row));
        int pos = line.start();
        while (pos < line.end() && this.x + PAD + UiText.width(this.text.substring(line.start(), pos + 1)) - 2 < mx) {
            pos++;
        }
        this.caret = pos;
        this.focusMillis = Util.getMillis();
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (!this.focused) {
            return false;
        }
        if (e.isPaste()) {
            final String clip = Minecraft.getInstance().keyboardHandler.getClipboard();
            if (clip != null) {
                this.insert(clip.replace("\r", "").replaceAll("[\\p{Cntrl}&&[^\n]]", ""));
            }
            return true;
        }
        switch (e.key()) {
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> this.insert("\n");
            case InputConstants.KEY_BACKSPACE -> {
                if (this.caret > 0) {
                    this.text = this.text.substring(0, this.caret - 1) + this.text.substring(this.caret);
                    this.caret--;
                    this.edited();
                }
            }
            case InputConstants.KEY_DELETE -> {
                if (this.caret < this.text.length()) {
                    this.text = this.text.substring(0, this.caret) + this.text.substring(this.caret + 1);
                    this.edited();
                }
            }
            case InputConstants.KEY_LEFT -> this.moveTo(Math.max(0, this.caret - 1));
            case InputConstants.KEY_RIGHT -> this.moveTo(Math.min(this.text.length(), this.caret + 1));
            case InputConstants.KEY_HOME -> this.moveTo(this.lines.get(this.lineOfCaret()).start());
            case InputConstants.KEY_END -> this.moveTo(this.lines.get(this.lineOfCaret()).end());
            case InputConstants.KEY_UP -> this.moveVertical(-1);
            case InputConstants.KEY_DOWN -> this.moveVertical(1);
            default -> {
                return !e.hasControlDown();
            }
        }
        return true;
    }

    private void moveVertical(int direction) {
        final int current = this.lineOfCaret();
        final int target = current + direction;
        if (target < 0 || target >= this.lines.size()) {
            return;
        }
        final Line from = this.lines.get(current);
        final int px = UiText.width(this.text.substring(from.start(), Math.min(this.caret, from.end())));
        final Line to = this.lines.get(target);
        int pos = to.start();
        while (pos < to.end() && UiText.width(this.text.substring(to.start(), pos + 1)) - 2 < px) {
            pos++;
        }
        this.moveTo(pos);
    }

    private void moveTo(int position) {
        this.caret = position;
        this.focusMillis = Util.getMillis();
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (!this.focused || !e.isAllowedChatCharacter()) {
            return false;
        }
        this.insert(e.codepointAsString());
        return true;
    }

    private void insert(String s) {
        if (s.isEmpty() || this.text.length() + s.length() > MAX_LENGTH) {
            return;
        }
        this.text = this.text.substring(0, this.caret) + s + this.text.substring(this.caret);
        this.caret += s.length();
        this.edited();
    }

    private void edited() {
        this.focusMillis = Util.getMillis();
        this.params.addProperty(this.spec.key(), this.text);
        this.changed();
    }

    @Override
    public void blur() {
        this.focused = false;
    }

    @Override
    public boolean isTextFocused() {
        return this.focused;
    }

}
