package de.vantrex.skysens.client.features.macro.node.action;

import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.action.ActionNodeType;
import de.vantrex.skysens.client.features.macro.action.ActionOutcome;
import de.vantrex.skysens.client.features.macro.action.ActionParams;
import de.vantrex.skysens.client.features.macro.action.MacroToasts;
import de.vantrex.skysens.client.features.macro.action.placeholder.PlaceholderRenderer;
import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.param.Choice;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.notification.Notification;
import de.vantrex.skysens.client.notification.NotificationDisplayTypeEnum;
import de.vantrex.skysens.client.notification.NotificationLevelEnum;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Shows a notification (title, chat or action bar). */
public final class ToastNode implements ActionNodeType {

    @Override
    public String id() {
        return "action.toast";
    }

    @Override
    public String displayName() {
        return "Show Toast";
    }

    @Override
    public String icon() {
        return "!";
    }

    @Override
    public List<ParamSpec> params() {
        return ActionParams.with(
                ParamSpec.of("title", "Title", "Main text. Supports {{ placeholders }}", ParamKind.TEMPLATE_TEXT, new JsonPrimitive("")),
                ParamSpec.of("subtitle", "Subtitle", "Second line (title display only). Supports {{ placeholders }}", ParamKind.TEMPLATE_TEXT, new JsonPrimitive("")),
                new ParamSpec("display", "Display", "Where to show it", ParamKind.ENUM, new JsonPrimitive("DEFAULT"),
                        () -> List.of(new Choice("DEFAULT", "Default"), new Choice("TITLE", "Title"), new Choice("CHAT", "Chat"), new Choice("ACTION_BAR", "Action bar")), null));
    }

    @Override
    public String summary(MacroNode node) {
        return ActionParams.shorten(Params.getString(node.params, "title", ""));
    }

    @Override
    public void validate(MacroNode node, List<NodeIssue> out) {
        if (Params.getString(node.params, "title", "").isBlank()) {
            out.add(new NodeIssue(NodeIssue.Severity.ERROR, node.id, "No title set"));
        }
    }

    @Override
    public ActionOutcome execute(MacroNode node, MacroContext ctx) {
        final List<String> unresolved = new ArrayList<>();
        final String title = PlaceholderRenderer.render(Params.getString(node.params, "title", ""), ctx, unresolved);
        final String subtitle = PlaceholderRenderer.render(Params.getString(node.params, "subtitle", ""), ctx, unresolved);
        if (title.isBlank()) {
            return ActionOutcome.failed("empty title");
        }
        final NotificationDisplayTypeEnum display = Params.getEnum(node.params, "display", NotificationDisplayTypeEnum.class, null);
        final Notification notification = Notification.builder()
                .text(Component.literal(title))
                .subtitle(subtitle.isBlank() ? null : Component.literal(subtitle))
                .displayType(display)
                .level(NotificationLevelEnum.IMPORTANT)
                .build();
        MacroToasts.show(notification);
        return ActionOutcome.ran();
    }

}
