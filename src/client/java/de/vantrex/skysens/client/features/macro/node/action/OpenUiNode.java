package de.vantrex.skysens.client.features.macro.node.action;

import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.action.ActionNodeType;
import de.vantrex.skysens.client.features.macro.action.ActionOutcome;
import de.vantrex.skysens.client.features.macro.action.ActionParams;
import de.vantrex.skysens.client.features.macro.action.SkysensScreenTarget;
import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.features.warp.WarpToMenuScreen;
import de.vantrex.skysens.client.ui.HudEditorScreen;
import de.vantrex.skysens.client.ui.SkysensConfigScreen;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

/** Opens one of SkySens' own screens. */
public final class OpenUiNode implements ActionNodeType {

    @Override
    public String id() {
        return "action.open_ui";
    }

    @Override
    public String displayName() {
        return "Open Screen";
    }

    @Override
    public String icon() {
        return "□";
    }

    @Override
    public List<ParamSpec> params() {
        final List<Choice> choices = new ArrayList<>();
        for (SkysensScreenTarget target : SkysensScreenTarget.values()) {
            choices.add(new Choice(target.name(), target.displayName()));
        }
        return ActionParams.with(new ParamSpec("target", "Screen", "Which screen to open", ParamKind.ENUM,
                new JsonPrimitive(SkysensScreenTarget.CONFIG.name()), () -> choices, null));
    }

    @Override
    public String summary(MacroNode node) {
        final SkysensScreenTarget target = Params.getEnum(node.params, "target", SkysensScreenTarget.class, null);
        return target == null ? "?" : target.displayName();
    }

    @Override
    public void validate(MacroNode node, List<NodeIssue> out) {
        final SkysensScreenTarget target = Params.getEnum(node.params, "target", SkysensScreenTarget.class, null);
        if (target == null) {
            out.add(new NodeIssue(NodeIssue.Severity.ERROR, node.id, "Unknown screen " + Params.getString(node.params, "target", "")));
        } else if (target == SkysensScreenTarget.MACRO_EDITOR) {
            out.add(new NodeIssue(NodeIssue.Severity.WARNING, node.id, "The macro editor does not exist yet"));
        }
    }

    @Override
    public ActionOutcome execute(MacroNode node, MacroContext ctx) {
        final SkysensScreenTarget target = Params.getEnum(node.params, "target", SkysensScreenTarget.class, null);
        if (target == null) {
            return ActionOutcome.failed("unknown screen");
        }
        final Minecraft mc = Minecraft.getInstance();
        switch (target) {
            case CONFIG -> mc.schedule(() -> mc.gui.setScreen(new SkysensConfigScreen(null)));
            case HUD_EDITOR -> HudEditorScreen.open();
            case WARP_MENU -> WarpToMenuScreen.open();
            case MACRO_EDITOR -> {
                return ActionOutcome.failed("the macro editor is not available yet");
            }
        }
        return ActionOutcome.ran();
    }

}
