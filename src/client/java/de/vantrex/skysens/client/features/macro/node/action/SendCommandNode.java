package de.vantrex.skysens.client.features.macro.node.action;

import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.action.ActionNodeType;
import de.vantrex.skysens.client.features.macro.action.ActionOutcome;
import de.vantrex.skysens.client.features.macro.action.ActionParams;
import de.vantrex.skysens.client.features.macro.action.MacroSender;
import de.vantrex.skysens.client.features.macro.action.placeholder.PlaceholderRenderer;
import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;

import java.util.ArrayList;
import java.util.List;

/** Runs a command, e.g. "/warp home". */
public final class SendCommandNode implements ActionNodeType {

    @Override
    public String id() {
        return "action.command";
    }

    @Override
    public String displayName() {
        return "Send Command";
    }

    @Override
    public String icon() {
        return "/";
    }

    @Override
    public List<ParamSpec> params() {
        return ActionParams.with(ParamSpec.of("command", "Command", "The command to run. Supports {{ placeholders }}", ParamKind.TEMPLATE_TEXT, new JsonPrimitive("")));
    }

    @Override
    public String summary(MacroNode node) {
        return ActionParams.shorten(Params.getString(node.params, "command", ""));
    }

    @Override
    public boolean sendsToServer() {
        return true;
    }

    @Override
    public void validate(MacroNode node, List<NodeIssue> out) {
        if (Params.getString(node.params, "command", "").isBlank()) {
            out.add(new NodeIssue(NodeIssue.Severity.ERROR, node.id, "No command set"));
        }
    }

    @Override
    public ActionOutcome execute(MacroNode node, MacroContext ctx) {
        final List<String> unresolved = new ArrayList<>();
        final String command = PlaceholderRenderer.render(Params.getString(node.params, "command", ""), ctx, unresolved);
        return MacroSender.sendCommand(command);
    }

}
