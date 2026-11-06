package de.vantrex.skysens.client.features.macro.editor.field;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.condition.ConditionType;
import de.vantrex.skysens.client.features.macro.condition.ConditionTypes;
import de.vantrex.skysens.client.features.macro.editor.HoverTip;
import de.vantrex.skysens.client.features.macro.editor.edit.GraphEdit;
import de.vantrex.skysens.client.features.macro.editor.history.EditNodeCommand;
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
            return PAD + DropdownControl.HEIGHT + (this.fields == null ? 0 : ParamFieldList.GAP + this.fields.height(inner)) + this.useHeight() + PAD;
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
            this.renderUse(g, ix, iy + DropdownControl.HEIGHT + (this.fields == null ? 0 : ParamFieldList.GAP + this.fields.height(iw)), iw, mx, my);
        }

        // ------------------------------------------------------------------ "Use held item"

        private static final int USE_GAP = 6;
        private static final int USE_H = 20;
        private static final int CHECK_H = 16;
        private static final long AVAILABILITY_MS = 300L;

        private boolean checklistOpen;
        private HeldItemAttributes.@Nullable Result checklist;
        private boolean[] ticked = new boolean[0];
        private HeldItemAttributes.@Nullable Result availability;
        private long availabilityTime;
        private int useY;

        private HeldItemAttributes.@Nullable Target useTarget() {
            return switch (this.type()) {
                case "item.held" -> HeldItemAttributes.Target.HELD;
                case "item.inventory" -> HeldItemAttributes.Target.INVENTORY;
                case "item.path" -> HeldItemAttributes.Target.PATH;
                default -> null;
            };
        }

        private int useHeight() {
            if (this.useTarget() == null) {
                return 0;
            }
            int h = USE_GAP + USE_H;
            if (this.checklistOpen && this.checklist != null) {
                h += USE_GAP + this.checklist.attributes().size() * CHECK_H + USE_GAP + USE_H;
            }
            return h;
        }

        private HeldItemAttributes.Result available(HeldItemAttributes.Target target) {
            final long now = System.currentTimeMillis();
            if (this.availability == null || now - this.availabilityTime > AVAILABILITY_MS) {
                this.availability = HeldItemAttributes.read(target);
                this.availabilityTime = now;
            }
            return this.availability;
        }

        private void renderUse(GuiGraphicsExtractor g, int ix, int y, int iw, int mx, int my) {
            final HeldItemAttributes.Target target = this.useTarget();
            if (target == null) {
                this.checklistOpen = false;
                return;
            }
            this.useY = y + USE_GAP;
            final HeldItemAttributes.Result result = this.available(target);
            final boolean enabled = result.disabledReason() == null;
            final boolean hover = RenderUtil.inside(mx, my, ix, this.useY, iw, USE_H);
            if (enabled) {
                FieldUi.button(g, ix, this.useY, iw, USE_H, "Use held item", mx, my);
            } else {
                RenderUtil.roundedRect(g, ix, this.useY, iw, USE_H, 4, Theme.CARD);
                UiText.drawCentered(g, "Use held item", ix + iw / 2, this.useY + (USE_H - 9) / 2 + 1, Theme.TEXT_FAINT);
                if (hover) {
                    HoverTip.set(result.disabledReason(), mx, my);
                }
                this.checklistOpen = false;
            }
            if (!this.checklistOpen || this.checklist == null) {
                return;
            }
            int cy = this.useY + USE_H + USE_GAP;
            final List<HeldItemAttributes.Attribute> attributes = this.checklist.attributes();
            for (int i = 0; i < attributes.size(); i++) {
                final HeldItemAttributes.Attribute attribute = attributes.get(i);
                final boolean on = this.ticked[i];
                RenderUtil.roundedRectWithBorder(g, ix + 2, cy + 3, 10, 10, 2, on ? Theme.ACCENT : Theme.INPUT, on ? Theme.ACCENT : Theme.BORDER);
                if (on) {
                    RenderUtil.rect(g, ix + 5, cy + 8, 4, 1, 0xFFFFFFFF);
                    RenderUtil.rect(g, ix + 7, cy + 6, 1, 4, 0xFFFFFFFF);
                }
                final String label = attribute.label() + ": ";
                UiText.draw(g, label, ix + 18, cy + 4, on ? Theme.TEXT : Theme.TEXT_MUTED);
                UiText.draw(g, UiText.ellipsize(attribute.value(), Math.max(10, iw - 22 - UiText.width(label))), ix + 18 + UiText.width(label), cy + 4,
                        on ? Theme.ACCENT : Theme.TEXT_FAINT);
                cy += CHECK_H;
            }
            cy += USE_GAP;
            final int half = (iw - 4) / 2;
            int count = 0;
            for (boolean b : this.ticked) {
                count += b ? 1 : 0;
            }
            FieldUi.button(g, ix, cy, half, USE_H, count == 0 ? "Add" : "Add " + count + (count == 1 ? " rule" : " rules"), mx, my);
            FieldUi.button(g, ix + half + 4, cy, iw - half - 4, USE_H, "Cancel", mx, my);
        }

        private boolean useClicked(double mx, double my) {
            final HeldItemAttributes.Target target = this.useTarget();
            if (target == null || my < this.useY) {
                return false;
            }
            final int ix = this.x + PAD;
            final int iw = this.w - 2 * PAD;
            if (RenderUtil.inside(mx, my, ix, this.useY, iw, USE_H)) {
                final HeldItemAttributes.Result result = HeldItemAttributes.read(target);
                if (result.disabledReason() == null) {
                    this.checklistOpen = !this.checklistOpen;
                    this.checklist = result;
                    this.ticked = new boolean[result.attributes().size()];
                    for (int i = 0; i < this.ticked.length; i++) {
                        this.ticked[i] = result.attributes().get(i).defaultTicked();
                    }
                    OptionWidget.playClick();
                }
                return true;
            }
            if (!this.checklistOpen || this.checklist == null) {
                return false;
            }
            int cy = this.useY + USE_H + USE_GAP;
            final int count = this.checklist.attributes().size();
            for (int i = 0; i < count; i++) {
                if (RenderUtil.inside(mx, my, ix, cy, iw, CHECK_H)) {
                    this.ticked[i] = !this.ticked[i];
                    OptionWidget.playClick();
                    return true;
                }
                cy += CHECK_H;
            }
            cy += USE_GAP;
            final int half = (iw - 4) / 2;
            if (RenderUtil.inside(mx, my, ix, cy, half, USE_H)) {
                this.applyChecklist(target);
                return true;
            }
            if (RenderUtil.inside(mx, my, ix + half + 4, cy, iw - half - 4, USE_H)) {
                this.checklistOpen = false;
                return true;
            }
            return false;
        }

        /** Turns the ticked attributes into rules next to this one (replacing it if it is still untouched). One undo step. */
        private void applyChecklist(HeldItemAttributes.Target target) {
            final List<HeldItemAttributes.Attribute> attributes = this.checklist == null ? List.of() : this.checklist.attributes();
            final List<JsonObject> created = new ArrayList<>();
            if (target == HeldItemAttributes.Target.INVENTORY) {
                final JsonObject merged = new JsonObject();
                for (int i = 0; i < attributes.size(); i++) {
                    if (this.ticked[i]) {
                        attributes.get(i).params().entrySet().forEach(e -> merged.add(e.getKey(), e.getValue()));
                    }
                }
                if (!merged.keySet().isEmpty()) {
                    created.add(HeldItemAttributes.rule("item.inventory", merged, GraphEdit::newRule));
                }
            } else {
                for (int i = 0; i < attributes.size(); i++) {
                    if (this.ticked[i]) {
                        created.add(HeldItemAttributes.rule(attributes.get(i).ruleType(), attributes.get(i).params(), GraphEdit::newRule));
                    }
                }
            }
            this.checklistOpen = false;
            if (created.isEmpty()) {
                return;
            }
            final List<JsonElement> list = this.owner.conditions.asList();
            final int index = indexOf(list, this.rule);
            final JsonElement params = this.rule.get("params");
            final boolean replace = index >= 0 && params != null && params.isJsonObject()
                    && !Params.getBool(this.rule, "negate", false) && HeldItemAttributes.isBlank(this.type(), params.getAsJsonObject());
            int at = index < 0 ? list.size() : index + 1;
            if (replace) {
                list.remove(index);
                this.owner.rows.remove(this.rule);
                at = index;
            }
            list.addAll(at, created);
            EditNodeCommand.breakMerge();
            this.owner.changed.run();
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
            if (this.useClicked(mx, my)) {
                return true;
            }
            return this.fields != null && this.fields.mouseClicked(mx, my, 0);
        }

    }

}
