package de.vantrex.skysens.client.features.macro;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.condition.RuleResult;
import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.features.macro.engine.ExecutionLog;
import de.vantrex.skysens.client.features.macro.engine.ExecutionRecord;
import de.vantrex.skysens.client.features.macro.engine.GraphWalker;
import de.vantrex.skysens.client.features.macro.engine.Resolution;
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
                        .executes(context -> debug()))
                .then(ClientCommands.literal("log")
                        .executes(context -> log()));
    }

    private static int reload() {
        final MacroService service = MacroService.getInstance();
        service.reload();
        ClientUtil.sendPrefixed("§7Macros reloaded: " + service.getWorkflows().size() + " workflows, " + service.nodeCount() + " nodes.");
        return 1;
    }

    private static int log() {
        final List<ExecutionRecord> records = ExecutionLog.getInstance().recent(10);
        if (records.isEmpty()) {
            ClientUtil.sendPrefixed("§7No macro key presses logged yet.");
            return 1;
        }
        final long now = System.currentTimeMillis();
        for (ExecutionRecord record : records) {
            final Resolution resolution = record.resolution();
            final GraphWalker.PathResult path = resolution.chosenPath();
            final String color = resolution.ran() ? "§a" : switch (resolution.outcome()) {
                case NO_ACTION -> "§7";
                case NO_MATCH_UNKNOWN_STATE -> "§c";
                default -> "§e";
            };
            final StringBuilder line = new StringBuilder("§7[" + (now - record.time()) / 1000 + "s ago] §f" + record.chord().displayName()
                    + " §8" + record.mode().name().toLowerCase().replace('_', ' ')
                    + (record.inGui() ? " (gui)" : "") + " " + color + outcomeLabel(resolution.outcome()));
            if (path != null) {
                line.append(" §7").append(path.ref().workflow().name).append(" -> ")
                        .append(NodeTypes.resolve(path.action().type).displayName())
                        .append(" \"").append(NodeTypes.resolve(path.action().type).summary(path.action())).append("\"");
            }
            if (resolution.message() != null) {
                line.append(" §8(").append(resolution.message()).append(")");
            }
            ClientUtil.sendPrefixed(line.toString());
            for (GraphWalker.PathResult p : resolution.paths()) {
                for (GraphWalker.LogicStep step : p.steps()) {
                    final StringBuilder rules = new StringBuilder("§8    " + p.ref().workflow().name + " / " + step.nodeName() + ": ");
                    for (RuleResult result : step.results()) {
                        rules.append(switch (result.value()) {
                            case TRUE -> "§a";
                            case FALSE -> "§7";
                            case UNKNOWN -> "§c";
                        }).append(result.ruleId()).append('=').append(result.value() == Tri.UNKNOWN ? "?" : result.value().name().toLowerCase());
                        if (result.unknownReason() != null) {
                            rules.append(" §8(").append(result.unknownReason()).append(')');
                        }
                        rules.append("§8, ");
                    }
                    rules.append("→ ").append(step.port() == null ? "end" : step.port());
                    ClientUtil.sendPrefixed(rules.toString());
                }
            }
        }
        return 1;
    }

    private static String outcomeLabel(Resolution.Outcome outcome) {
        return switch (outcome) {
            case RAN -> "Ran";
            case RATE_LIMITED -> "Rate limited";
            case ON_COOLDOWN -> "On cooldown";
            case FAILED -> "Failed";
            case NO_ACTION -> "No action";
            case NO_MATCH_UNKNOWN_STATE -> "Unknown state";
        };
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
