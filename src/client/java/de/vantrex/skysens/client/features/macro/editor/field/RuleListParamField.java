package de.vantrex.skysens.client.features.macro.editor.field;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.macro.editor.edit.GraphEdit;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.ui.SearchField;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import de.vantrex.skysens.client.ui.widget.OptionWidget;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * RULE_LIST parameter. For the If node ({@code conditions}) it is a flat condition list; for the Switch node
 * ({@code rules}) it is a list of rule cards, each with a name (the output label), AND/OR and its own conditions.
 * Adding, removing or reordering rules changes a Switch node's outputs, so the owner prunes dangling wires.
 */
public final class RuleListParamField extends ParamField {

    private static final int PAD = 6;
    private static final int HEADER_H = 22;
    private static final int ADD_H = 22;
    private static final int CARD_GAP = 8;

    private static final List<Choice> COMBINATORS = List.of(new Choice("AND", "All (AND)"), new Choice("OR", "Any (OR)"));

    private final boolean groups;
    private final JsonArray array;
    private final ConditionListEditor flat;
    private final Map<JsonObject, Group> cards = new IdentityHashMap<>();
    private final List<Group> laidOut = new ArrayList<>();
    private int addX;
    private int addY;
    private int addW;

    public RuleListParamField(ParamSpec spec, JsonObject params, FieldContext ctx) {
        super(spec, params, ctx);
        this.groups = "rules".equals(spec.key());
        final JsonElement existing = params.get(spec.key());
        if (existing != null && existing.isJsonArray()) {
            this.array = existing.getAsJsonArray();
        } else {
            this.array = new JsonArray();
            params.add(spec.key(), this.array);
        }
        this.flat = this.groups ? null : new ConditionListEditor(this.array, ctx, this::changed);
    }

    private List<JsonObject> groupObjects() {
        final List<JsonObject> out = new ArrayList<>();
        for (JsonElement e : this.array) {
            if (e.isJsonObject()) {
                out.add(e.getAsJsonObject());
            }
        }
        return out;
    }

    private Group card(JsonObject group) {
        return this.cards.computeIfAbsent(group, g -> new Group(this, g));
    }

    @Override
    public int height(int w) {
        if (!this.groups) {
            return this.headerHeight(w) + this.flat.height(w);
        }
        int h = this.headerHeight(w);
        for (JsonObject group : this.groupObjects()) {
            h += this.card(group).height(w) + CARD_GAP;
        }
        return h + ADD_H;
    }

    @Override
    public void render(GuiGraphicsExtractor g, int x, int y, int w, int mx, int my) {
        int cy = this.renderHeader(g, x, y, w);
        if (!this.groups) {
            this.flat.render(g, x, cy, w, mx, my);
            return;
        }
        this.laidOut.clear();
        int index = 1;
        for (JsonObject group : this.groupObjects()) {
            final Group card = this.card(group);
            card.render(g, x, cy, w, index++, mx, my);
            this.laidOut.add(card);
            cy += card.height(w) + CARD_GAP;
        }
        this.addX = x;
        this.addY = cy;
        this.addW = w;
        FieldUi.button(g, x, cy, w, ADD_H, "+ Add rule", mx, my);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (!this.groups) {
            return this.flat.mouseClicked(mx, my, button);
        }
        this.blur();
        if (button != 0) {
            return false;
        }
        if (RenderUtil.inside(mx, my, this.addX, this.addY, this.addW, ADD_H)) {
            this.array.add(GraphEdit.newSwitchRule());
            OptionWidget.playClick();
            this.changed();
            return true;
        }
        for (Group card : new ArrayList<>(this.laidOut)) {
            if (card.mouseClicked(mx, my)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(KeyEvent e) {
        if (!this.groups) {
            return this.flat.keyPressed(e);
        }
        for (Group card : this.laidOut) {
            if (card.name.isFocused() ? card.name.keyPressed(e) : card.conditions.keyPressed(e)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent e) {
        if (!this.groups) {
            return this.flat.charTyped(e);
        }
        for (Group card : this.laidOut) {
            if (card.name.isFocused() ? card.name.charTyped(e) : card.conditions.charTyped(e)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void blur() {
        if (!this.groups) {
            this.flat.blur();
            return;
        }
        for (Group card : this.cards.values()) {
            card.name.setFocused(false);
            card.conditions.blur();
        }
    }

    @Override
    public boolean isTextFocused() {
        if (!this.groups) {
            return this.flat.isTextFocused();
        }
        for (Group card : this.cards.values()) {
            if (card.name.isFocused() || card.conditions.isTextFocused()) {
                return true;
            }
        }
        return false;
    }

    private void move(JsonObject group, int delta) {
        final List<JsonElement> list = this.array.asList();
        int index = -1;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == group) {
                index = i;
            }
        }
        final int target = index + delta;
        if (index < 0 || target < 0 || target >= list.size()) {
            return;
        }
        list.remove(index);
        list.add(target, group);
        this.changed();
    }

    private void remove(JsonObject group) {
        final List<JsonElement> list = this.array.asList();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == group) {
                list.remove(i);
                this.cards.remove(group);
                this.changed();
                return;
            }
        }
    }

    /** One Switch rule card. */
    private static final class Group {

        private final RuleListParamField owner;
        private final JsonObject json;
        private final SearchField name;
        private final DropdownControl combinator;
        private final ConditionListEditor conditions;
        private boolean initialising = true;
        private int x;
        private int y;
        private int w;

        Group(RuleListParamField owner, JsonObject json) {
            this.owner = owner;
            this.json = json;
            this.name = new SearchField(text -> {
                if (!this.initialising) {
                    json.addProperty("name", text);
                    owner.changed();
                }
            }).noHint().maxLength(24);
            this.name.setText(Params.getString(json, "name", ""));
            this.name.setFocused(false);
            this.initialising = false;
            this.combinator = DropdownControl.single(() -> COMBINATORS, () -> Params.getString(json, "combinator", "AND"), value -> {
                json.addProperty("combinator", value);
                owner.changed();
            });
            JsonArray list = json.has("conditions") && json.get("conditions").isJsonArray() ? json.getAsJsonArray("conditions") : null;
            if (list == null) {
                list = new JsonArray();
                json.add("conditions", list);
            }
            this.conditions = new ConditionListEditor(list, owner.ctx.nested(key -> owner.changed()), owner::changed);
        }

        int height(int w) {
            return PAD + HEADER_H + 6 + this.conditions.height(w - 2 * PAD) + PAD;
        }

        void render(GuiGraphicsExtractor g, int x, int y, int w, int index, int mx, int my) {
            this.x = x;
            this.y = y;
            this.w = w;
            RenderUtil.roundedRectWithBorder(g, x, y, w, this.height(w), Theme.CARD_RADIUS, Theme.CARD, Theme.BORDER);
            final int ix = x + PAD;
            final int iw = w - 2 * PAD;
            final int iy = y + PAD;
            final int buttons = 3 * (FieldUi.ICON + 2);
            final int comboW = 84;
            this.name.placeholder("Rule " + index);
            this.name.layout(ix, iy, iw - buttons - comboW - 8, HEADER_H);
            this.name.render(g, mx, my);
            this.combinator.render(g, ix + iw - buttons - comboW - 4, iy + (HEADER_H - DropdownControl.HEIGHT) / 2, comboW, mx, my);
            final int bx = ix + iw - buttons + 2;
            final int by = iy + (HEADER_H - FieldUi.ICON) / 2;
            FieldUi.iconButton(g, bx, by, FieldUi.Icon.UP, true, mx, my);
            FieldUi.iconButton(g, bx + FieldUi.ICON + 2, by, FieldUi.Icon.DOWN, true, mx, my);
            FieldUi.iconButton(g, bx + 2 * (FieldUi.ICON + 2), by, FieldUi.Icon.CLOSE, true, mx, my);
            this.conditions.render(g, ix, iy + HEADER_H + 6, iw, mx, my);
        }

        boolean mouseClicked(double mx, double my) {
            if (!RenderUtil.inside(mx, my, this.x, this.y, this.w, this.height(this.w))) {
                return false;
            }
            final int ix = this.x + PAD;
            final int iw = this.w - 2 * PAD;
            final int iy = this.y + PAD;
            final int buttons = 3 * (FieldUi.ICON + 2);
            final int comboW = 84;
            if (this.name.mouseClicked(mx, my)) {
                return true;
            }
            if (this.combinator.mouseClicked(mx, my)) {
                return true;
            }
            final int bx = ix + iw - buttons + 2;
            final int by = iy + (HEADER_H - FieldUi.ICON) / 2;
            if (FieldUi.inIcon(mx, my, bx, by)) {
                this.owner.move(this.json, -1);
                return true;
            }
            if (FieldUi.inIcon(mx, my, bx + FieldUi.ICON + 2, by)) {
                this.owner.move(this.json, 1);
                return true;
            }
            if (FieldUi.inIcon(mx, my, bx + 2 * (FieldUi.ICON + 2), by)) {
                this.owner.remove(this.json);
                return true;
            }
            if (my >= iy + HEADER_H + 6) {
                return this.conditions.mouseClicked(mx, my, 0);
            }
            return comboW < 0;
        }

    }

}
