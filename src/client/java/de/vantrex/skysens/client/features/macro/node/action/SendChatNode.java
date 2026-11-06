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
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;

import java.util.ArrayList;
import java.util.List;

/** Sends a chat message to all chat, the party or the co-op. */
public final class SendChatNode implements ActionNodeType {

    /** Persisted by name in params. Never rename. */
    public enum Channel { ALL, PARTY, COOP }

    @Override
    public String id() {
        return "action.chat";
    }

    @Override
    public String displayName() {
        return "Send Chat";
    }

    @Override
    public String icon() {
        return "C";
    }

    @Override
    public List<ParamSpec> params() {
        return ActionParams.with(
                new ParamSpec("channel", "Channel", "Where to send the message", ParamKind.ENUM, new JsonPrimitive(Channel.ALL.name()),
                        () -> List.of(new Choice("ALL", "All chat"), new Choice("PARTY", "Party"), new Choice("COOP", "Co-op")), null),
                ParamSpec.of("message", "Message", "The message. Supports {{ placeholders }}", ParamKind.TEMPLATE_TEXT, new JsonPrimitive("")));
    }

    @Override
    public String summary(MacroNode node) {
        return ActionParams.shorten(Params.getString(node.params, "message", ""));
    }

    @Override
    public boolean sendsToServer() {
        return true;
    }

    @Override
    public void validate(MacroNode node, List<NodeIssue> out) {
        if (Params.getString(node.params, "message", "").isBlank()) {
            out.add(new NodeIssue(NodeIssue.Severity.ERROR, node.id, "No message set"));
        }
    }

    @Override
    public ActionOutcome execute(MacroNode node, MacroContext ctx) {
        final List<String> unresolved = new ArrayList<>();
        final String message = PlaceholderRenderer.render(Params.getString(node.params, "message", ""), ctx, unresolved);
        if (!unresolved.isEmpty()) {
            // never send a half-filled message
            return ActionOutcome.failed(PlaceholderRenderer.notKnownMessage(unresolved));
        }
        final Channel channel = Params.getEnum(node.params, "channel", Channel.class, Channel.ALL);
        return switch (channel) {
            case ALL -> MacroSender.sendChat(message);
            case PARTY -> message.startsWith("/") ? ActionOutcome.failed("chat messages must not start with /") : MacroSender.sendCommand("pc " + message);
            case COOP -> message.startsWith("/") ? ActionOutcome.failed("chat messages must not start with /") : MacroSender.sendCommand("cc " + message);
        };
    }

}
