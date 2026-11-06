package de.vantrex.skysens.client.features.macro.editor.field;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.features.macro.engine.InputChord;
import de.vantrex.skysens.client.features.macro.engine.InputDevice;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.trigger.HotkeyTriggerNode;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import de.vantrex.skysens.client.ui.widget.OptionWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * KEYBIND parameter. Click the button, then press a key or mouse button (side buttons included) together with any
 * Ctrl/Shift/Alt. A modifier key alone is bound when it is released without another key. Esc cancels. While capturing,
 * the editor screen routes raw events here through the static methods.
 */
public final class KeybindParamField extends ParamField {

    private static final int BUTTON_H = 22;

    private static @Nullable KeybindParamField capturing;
    private static int pendingModifier = -1;

    private int bx;
    private int by;
    private int bw;
    private int clearX;

    public KeybindParamField(ParamSpec spec, JsonObject params, FieldContext ctx) {
        super(spec, params, ctx);
    }

    public static boolean isCapturing() {
        return capturing != null;
    }

    public static void cancelCapture() {
        capturing = null;
        pendingModifier = -1;
    }

    private static boolean isModifierKey(int key) {
        return key == InputConstants.KEY_LCONTROL || key == InputConstants.KEY_RCONTROL || key == InputConstants.KEY_LSHIFT
                || key == InputConstants.KEY_RSHIFT || key == InputConstants.KEY_LALT || key == InputConstants.KEY_RALT;
    }

    /** A key press while capturing. Always consumed. */
    public static boolean captureKey(KeyEvent e) {
        final KeybindParamField field = capturing;
        if (field == null) {
            return false;
        }
        if (e.isEscape()) {
            cancelCapture();
            return true;
        }
        if (isModifierKey(e.key())) {
            pendingModifier = e.key();
            return true;
        }
        field.bind(InputChord.of(InputDevice.KEYBOARD, e.key(), e));
        return true;
    }

    public static boolean captureKeyRelease(KeyEvent e) {
        final KeybindParamField field = capturing;
        if (field == null) {
            return false;
        }
        if (pendingModifier == e.key()) {
            field.bind(InputChord.of(InputDevice.KEYBOARD, e.key(), e));
        }
        return true;
    }

    public static boolean captureMouse(MouseButtonEvent e) {
        final KeybindParamField field = capturing;
        if (field == null) {
            return false;
        }
        field.bind(InputChord.of(InputDevice.MOUSE, e.button(), (InputWithModifiers) e));
        return true;
    }

    private void bind(InputChord chord) {
        this.params.add(this.spec.key(), chord.toJson());
        cancelCapture();
        OptionWidget.playClick();
        this.changed();
    }

    private @Nullable InputChord chord() {
        return InputChord.fromJson(this.params.get(this.spec.key()));
    }

    /** Names of other workflows with a trigger on the same chord. */
    private List<String> sameChord(@Nullable InputChord chord) {
        final List<String> out = new ArrayList<>();
        if (chord == null) {
            return out;
        }
        for (Workflow workflow : this.ctx.all()) {
            for (MacroNode node : workflow.nodes) {
                if (node == this.ctx.node() || !NodeTypes.resolve(node.type).id().equals(HotkeyTriggerNode.ID)) {
                    continue;
                }
                final HotkeyTriggerNode.Spec spec = HotkeyTriggerNode.spec(node);
                if (spec != null && spec.chord().equals(chord) && !out.contains(workflow.name)) {
                    out.add(workflow.name);
                }
            }
        }
        return out;
    }

    private List<FormattedCharSequence> conflictLines(int w) {
        final List<String> names = this.sameChord(this.chord());
        if (names.isEmpty()) {
            return List.of();
        }
        return UiText.wrap("Also used by " + String.join(", ", names) + ". The macro higher in the list wins.", w);
    }

    @Override
    public int height(int w) {
        final int lines = this.conflictLines(w).size();
        return this.headerHeight(w) + BUTTON_H + (lines == 0 ? 0 : 3 + lines * (UiText.lineHeight() + 1));
    }

    @Override
    public void render(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my) {
        final int cy = this.renderHeader(g, x, y, w);
        this.bx = x;
        this.by = cy;
        this.bw = w - BUTTON_H - 4;
        this.clearX = x + w - BUTTON_H;
        final boolean capture = capturing == this;
        final boolean hover = RenderUtil.inside(mx, my, this.bx, this.by, this.bw, BUTTON_H);
        final int border = capture ? Theme.ACCENT : hover ? Theme.TEXT_FAINT : Theme.BORDER;
        RenderUtil.inputBox(g, this.bx, this.by, this.bw, BUTTON_H, 4, capture || hover, border, border);
        final InputChord chord = this.chord();
        final String text = capture ? "Press a key or mouse button…  (Esc cancels)" : chord == null ? "Click to bind" : chord.displayName();
        UiText.drawCentered(g, UiText.ellipsize(text, this.bw - 8), this.bx + this.bw / 2, this.by + (BUTTON_H - 9) / 2 + 1,
                capture ? Theme.ACCENT : chord == null ? Theme.TEXT_FAINT : Theme.TEXT);
        // clear button
        RenderUtil.roundedRect(g, this.clearX, this.by, BUTTON_H, BUTTON_H, 4, Theme.CARD);
        FieldUi.iconButton(g, this.clearX + (BUTTON_H - FieldUi.ICON) / 2, this.by + (BUTTON_H - FieldUi.ICON) / 2, FieldUi.Icon.CLOSE, chord != null, mx, my);

        int ly = this.by + BUTTON_H + 3;
        for (FormattedCharSequence line : this.conflictLines(w)) {
            UiText.drawLine(g, line, x, ly, FieldUi.AMBER);
            ly += UiText.lineHeight() + 1;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) {
            return false;
        }
        if (RenderUtil.inside(mx, my, this.bx, this.by, this.bw, BUTTON_H)) {
            capturing = this;
            pendingModifier = -1;
            OptionWidget.playClick();
            return true;
        }
        if (RenderUtil.inside(mx, my, this.clearX, this.by, BUTTON_H, BUTTON_H) && this.chord() != null) {
            this.params.add(this.spec.key(), new InputChord(InputDevice.KEYBOARD, -1, 0).toJson());
            OptionWidget.playClick();
            this.changed();
            return true;
        }
        return false;
    }

    @Override
    public void blur() {
        if (capturing == this) {
            cancelCapture();
        }
    }

}
