package de.vantrex.skysens.client.features.macro.node.action;

import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.macro.action.ActionNodeType;
import de.vantrex.skysens.client.features.macro.action.ActionOutcome;
import de.vantrex.skysens.client.features.macro.action.ActionParams;
import de.vantrex.skysens.client.features.macro.action.MacroToasts;
import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.notification.Notification;
import de.vantrex.skysens.client.notification.NotificationLevelEnum;
import de.vantrex.skysens.client.ui.model.ConfigTree;
import de.vantrex.skysens.client.ui.model.OptionKind;
import de.vantrex.skysens.client.ui.model.OptionNode;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Flips a boolean SkySens option, addressed by its config path (e.g. qolCategory.junkHighlight.enabled). */
public final class ToggleOptionNode implements ActionNodeType {

    /** Toggle options by path. Built on first use: walking the config tree is not cheap. */
    private static @Nullable Map<String, OptionNode> toggles;

    private static synchronized @Nullable OptionNode find(String path) {
        if (toggles == null) {
            final Map<String, OptionNode> map = new HashMap<>();
            for (OptionNode option : ConfigTree.allLeaves(ConfigTree.build())) {
                if (option.kind() == OptionKind.TOGGLE && option.get() instanceof Boolean) {
                    map.put(option.option().getPath(), option);
                }
            }
            toggles = map;
        }
        return toggles.get(path);
    }

    @Override
    public String id() {
        return "action.toggle_option";
    }

    @Override
    public String displayName() {
        return "Toggle Option";
    }

    @Override
    public String icon() {
        return "T";
    }

    @Override
    public List<ParamSpec> params() {
        return ActionParams.with(ParamSpec.of("option", "Option", "The SkySens on/off option to flip", ParamKind.CONFIG_TOGGLE, new JsonPrimitive("")));
    }

    @Override
    public String summary(MacroNode node) {
        return ActionParams.shorten(Params.getString(node.params, "option", ""));
    }

    @Override
    public void validate(MacroNode node, List<NodeIssue> out) {
        if (Params.getString(node.params, "option", "").isBlank()) {
            out.add(new NodeIssue(NodeIssue.Severity.WARNING, node.id, "No option chosen"));
        }
    }

    @Override
    public ActionOutcome execute(MacroNode node, MacroContext ctx) {
        final String path = Params.getString(node.params, "option", "");
        final OptionNode option = find(path);
        if (option == null) {
            return ActionOutcome.failed("unknown option \"" + path + "\"");
        }
        final boolean next = !Boolean.TRUE.equals(option.get());
        option.set(next);
        SkysensConfig.getInstance().saveNow();
        MacroToasts.show(Notification.builder()
                .text(Component.literal(option.name() + ": " + (next ? "on" : "off")))
                .level(NotificationLevelEnum.IMPORTANT)
                .build());
        return ActionOutcome.ran();
    }

}
