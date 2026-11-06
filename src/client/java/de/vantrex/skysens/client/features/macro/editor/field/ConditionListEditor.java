package de.vantrex.skysens.client.features.macro.editor.field;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ConditionTypes;
import de.vantrex.skysens.client.features.macro.editor.edit.GraphEdit;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.render.UiText;
import de.vantrex.skysens.client.ui.widget.OptionWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Edits an array of condition rules {@code {id, type, negate, params}}: per row a type dropdown, a "Not" checkbox,
 * move up / down / remove buttons and the condition's own params as fields (dropdowns where there are choices).
 */
public final class ConditionListEditor {

    private static final int PAD = 6;
    private static final int ADD_H = 20;
    private static final int ROW_GAP = 6;

    private final JsonArray conditions;
    private final FieldContext ctx;
    private final Runnable changed;
    private final Map<JsonObject, Row> rows = new IdentityHashMap<>();
    private final List<Row> laidOut = new ArrayList<>();
    private int addX;
    private int addY;
    private int addW;

    /** @param changed called after any edit (params or structure) */
    public ConditionListEditor(JsonArray conditions, FieldContext ctx, Runnable changed) {
        this.conditions = conditions;
        this.ctx = ctx;
        this.changed = changed;
    }

    private static List<Choice> typeChoices() {
        final List<Choice> out = new ArrayList<>();
        for (ConditionType type : ConditionTypes.all()) {
            out.add(new Choice(type.id(), type.displayName()));
        }
        return out;
    }

    private List<JsonObject> rules() {
        final List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : this.conditions) {
            if (e.isJsonObject()) {
                out.add(e.getAsJsonObject());
            }
        }
        return out;
    }

    private Row row(JsonObject rule) {
        return this.rows.computeIfAbsent(rule, r -> new Row(this, r));
    }

    public int height(int w) {
        int h = 0;
        for (JsonObject rule : this.rules()) {
            h += this.row(rule).height(w) + ROW_GAP;
        }
        return h + ADD_H;
    }

    public void render(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my) {
        this.laidOut.clear();
        int cy = y;
        for (JsonObject rule : this.rules()) {
            final Row row = this.row(rule);
            row.render(g, x, cy, w, mx, my);
            this.laidOut.add(row);
            cy += row.height(w) + ROW_GAP;
        }
        this.addX = x;
        this.addY = cy;
        this.addW = w;
        FieldUi.button(g, x, cy, w, ADD_H, "+ Add condition", mx, my);
    }

    public boolean mouseClicked(double mx, double my, int button) {
        this.blur();
        if (button != 0) {
            return false;
        }
        if (RenderUtil.inside(mx, my, this.addX, this.addY, this.addW, ADD_H)) {
            final ConditionType first = ConditionTypes.all().iterator().next();
            this.conditions.add(GraphEdit.newRule(first));
            OptionWidget.playClick();
            this.changed.run();
            return true;
        }
        for (Row row : new ArrayList<>(this.laidOut)) {
            if (row.mouseClicked(mx, my)) {
                return true;
            }
        }
        return false;
    }

    public boolean keyPressed(KeyEvent e) {
        for (Row row : this.laidOut) {
            if (row.fields != null && row.fields.keyPressed(e)) {
                return true;
            }
        }
        return false;
    }

    public boolean charTyped(CharacterEvent e) {
        for (Row row : this.laidOut) {
            if (row.fields != null && row.fields.charTyped(e)) {
                return true;
            }
        }
        return false;
    }

    public void blur() {
        for (Row row : this.rows.values()) {
            if (row.fields != null) {
                row.fields.blur();
            }
        }
    }

    public boolean isTextFocused() {
        for (Row row : this.rows.values()) {
            if (row.fields != null && row.fields.isTextFocused()) {
                return true;
            }
        }
        return false;
    }

    private void move(JsonObject rule, int delta) {
        final List<JsonElement> list = this.conditions.asList();
        final int index = indexOf(list, rule);
        final int target = index + delta;
        if (index < 0 || target < 0 || target >= list.size()) {
            return;
        }
        list.remove(index);
        list.add(target, rule);
        this.changed.run();
    }

    private void remove(JsonObject rule) {
        final List<JsonElement> list = this.conditions.asList();
        final int index = indexOf(list, rule);
        if (index >= 0) {
            list.remove(index);
            this.rows.remove(rule);
            this.changed.run();
        }
    }

    private static int indexOf(List<JsonElement> list, JsonObject rule) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == rule) {
                return i;
            }
        }
        return -1;
    }

    private static final class Row {

        private final ConditionListEditor owner;
        private final JsonObject rule;
        private final DropdownControl typeDrop;
        private @Nullable ParamFieldList fields;
        private @Nullable String builtType;
        private int x;
        private int y;
        private int w;

        Row(ConditionListEditor owner, JsonObject rule) {
            this.owner = owner;
            this.rule = rule;
            this.typeDrop = DropdownControl.single(ConditionListEditor::typeChoices, () -> Params.getString(rule, "type", ""), this::setType);
        }

        private String type() {
            return Params.getString(this.rule, "type", "");
        }

        private void setType(String id) {
            final ConditionType type = ConditionTypes.get(id);
            this.rule.addProperty("type", id);
            this.rule.add("params", type == null ? new JsonObject() : ParamSpec.defaults(type.params()));
            this.owner.changed.run();
        }

        private void ensureFields() {
            final String type = this.type();
            if (this.fields != null && type.equals(this.builtType)) {
                return;
            }
            this.builtType = type;
            final ConditionType def = ConditionTypes.get(type);
            if (def == null) {
                this.fields = null;
                return;
            }
            final JsonElement existing = this.rule.get("params");
            final JsonObject params;
            if (existing != null && existing.isJsonObject()) {
                params = existing.getAsJsonObject();
            } else {
                params = new JsonObject();
                this.rule.add("params", params);
            }
            final FieldContext nested = this.owner.ctx.nested(key -> {
                // changing the island invalidates the chosen zones
                if ("island".equals(key) && params.has("zones")) {
                    params.add("zones", new JsonArray());
                }
                this.owner.changed.run();
            });
            this.fields = new ParamFieldList(def.params(), params, nested);
        }

        int height(int w) {
            this.ensureFields();
            final int inner = w - 2 * PAD;
            return PAD + DropdownControl.HEIGHT + (this.fields == null ? 0 : ParamFieldList.GAP + this.fields.height(inner)) + PAD;
        }

        void render(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my) {
            this.ensureFields();
            this.x = x;
            this.y = y;
            this.w = w;
            RenderUtil.roundedRect(g, x, y, w, this.height(w), Theme.CARD_RADIUS, Theme.CARD_NESTED);
            final int ix = x + PAD;
            final int iw = w - 2 * PAD;
            final int iy = y + PAD;
            final int buttons = 3 * (FieldUi.ICON + 2);
            final int notW = 10 + 4 + UiText.width("Not") + 6;
            this.typeDrop.render(g, ix, iy, iw - buttons - notW, mx, my);
            // Not checkbox
            final boolean negate = Params.getBool(this.rule, "negate", false);
            final int nx = ix + iw - buttons - notW + 6;
            RenderUtil.roundedRectWithBorder(g, nx, iy + 5, 10, 10, 2, negate ? Theme.ACCENT : Theme.INPUT, negate ? Theme.ACCENT : Theme.BORDER);
            if (negate) {
                RenderUtil.rect(g, nx + 2, iy + 10, 3, 1, 0xFFFFFFFF);
                RenderUtil.rect(g, nx + 4, iy + 8, 1, 3, 0xFFFFFFFF);
            }
            UiText.draw(g, "Not", nx + 14, iy + (DropdownControl.HEIGHT - 9) / 2 + 1, negate ? Theme.TEXT : Theme.TEXT_MUTED);
            int bx = ix + iw - buttons + 2;
            final int by = iy + (DropdownControl.HEIGHT - FieldUi.ICON) / 2;
            FieldUi.iconButton(g, bx, by, FieldUi.Icon.UP, true, mx, my);
            FieldUi.iconButton(g, bx + FieldUi.ICON + 2, by, FieldUi.Icon.DOWN, true, mx, my);
            FieldUi.iconButton(g, bx + 2 * (FieldUi.ICON + 2), by, FieldUi.Icon.CLOSE, true, mx, my);
            if (this.fields != null) {
                this.fields.render(g, ix, iy + DropdownControl.HEIGHT + ParamFieldList.GAP, iw, mx, my);
            }
        }

        boolean mouseClicked(double mx, double my) {
            if (!RenderUtil.inside(mx, my, this.x, this.y, this.w, this.height(this.w))) {
                return false;
            }
            final int ix = this.x + PAD;
            final int iw = this.w - 2 * PAD;
            final int iy = this.y + PAD;
            final int buttons = 3 * (FieldUi.ICON + 2);
            final int notW = 10 + 4 + UiText.width("Not") + 6;
            if (this.typeDrop.mouseClicked(mx, my)) {
                return true;
            }
            if (RenderUtil.inside(mx, my, ix + iw - buttons - notW + 2, iy, notW + 2, DropdownControl.HEIGHT)) {
                this.rule.add("negate", new JsonPrimitive(!Params.getBool(this.rule, "negate", false)));
                OptionWidget.playClick();
                this.owner.changed.run();
                return true;
            }
            final int bx = ix + iw - buttons + 2;
            final int by = iy + (DropdownControl.HEIGHT - FieldUi.ICON) / 2;
            if (FieldUi.inIcon(mx, my, bx, by)) {
                this.owner.move(this.rule, -1);
                return true;
            }
            if (FieldUi.inIcon(mx, my, bx + FieldUi.ICON + 2, by)) {
                this.owner.move(this.rule, 1);
                return true;
            }
            if (FieldUi.inIcon(mx, my, bx + 2 * (FieldUi.ICON + 2), by)) {
                this.owner.remove(this.rule);
                return true;
            }
            return this.fields != null && this.fields.mouseClicked(mx, my, 0);
        }

    }

}
