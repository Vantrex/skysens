package de.vantrex.skysens.client.features.macro;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.util.ClientUtil;
import net.fabricmc.fabric.api.client.command.v2.ClientCommands;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.List;

/** The /skysens macros subtree. */
public final class MacroCommands {

    private MacroCommands() {
    }

    public static LiteralArgumentBuilder<FabricClientCommandSource> node() {
        return ClientCommands.literal("macros")
                .then(ClientCommands.literal("reload")
                        .executes(context -> reload()))
                .then(ClientCommands.literal("debug")
                        .executes(context -> debug()));
    }

    private static int reload() {
        final MacroService service = MacroService.getInstance();
        service.reload();
        ClientUtil.sendPrefixed("§7Macros reloaded: " + service.getWorkflows().size() + " workflows, " + service.nodeCount() + " nodes.");
        return 1;
    }

    private static int debug() {
        final MacroService service = MacroService.getInstance();
        final List<Workflow> workflows = service.getWorkflows();
        int issueCount = 0;
        ClientUtil.sendPrefixed("§7Macros: " + workflows.size() + " workflows, " + service.nodeCount() + " nodes ("
                + service.unknownNodeCount() + " unknown), " + service.getTriggerIndex().size() + " indexed triggers.");
        for (Workflow workflow : workflows) {
            final List<NodeIssue> issues = service.issues(workflow);
            issueCount += issues.size();
            ClientUtil.sendPrefixed("§7- \"" + workflow.name + "\" (" + (workflow.active ? "active" : "inactive") + "): "
                    + workflow.nodes.size() + " nodes, " + workflow.connections.size() + " wires, " + issues.size() + " issues");
            for (MacroNode node : workflow.nodes) {
                if (!NodeTypes.isKnown(node.type)) {
                    ClientUtil.sendPrefixed("§c    unknown node " + node.id + " (type \"" + node.type + "\")");
                }
            }
            for (NodeIssue issue : issues) {
                ClientUtil.sendPrefixed("§e    " + issue.severity() + ": " + issue.message());
            }
        }
        ClientUtil.sendPrefixed("§7Total issues: " + issueCount);
        return 1;
    }

}
