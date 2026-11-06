package de.vantrex.skysens.client.features.macro.analysis;

import de.vantrex.skysens.client.features.macro.model.Workflow;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** The issues of every workflow of one validation run, by workflow id and node id. Immutable once built. */
public final class ValidationResult {

    public static final ValidationResult EMPTY = new ValidationResult();

    private final Map<String, List<NodeIssue>> byWorkflow = new HashMap<>();
    private final Map<String, Map<String, List<NodeIssue>>> byNode = new HashMap<>();

    void put(String workflowId, List<NodeIssue> issues) {
        this.byWorkflow.put(workflowId, issues);
        final Map<String, List<NodeIssue>> nodes = new LinkedHashMap<>();
        for (NodeIssue issue : issues) {
            nodes.computeIfAbsent(issue.nodeId(), k -> new ArrayList<>()).add(issue);
        }
        this.byNode.put(workflowId, nodes);
    }

    /** All issues of the workflow, in the order they were found. */
    public List<NodeIssue> issues(Workflow workflow) {
        return this.byWorkflow.getOrDefault(workflow.id, List.of());
    }

    /** The issues of the workflow grouped by node id. */
    public Map<String, List<NodeIssue>> byNode(Workflow workflow) {
        return this.byNode.getOrDefault(workflow.id, Map.of());
    }

    public List<NodeIssue> forNode(Workflow workflow, String nodeId) {
        return this.byNode(workflow).getOrDefault(nodeId, List.of());
    }

    /** The keybind conflicts of the workflow (header chip). */
    public List<NodeIssue> conflicts(Workflow workflow) {
        final List<NodeIssue> out = new ArrayList<>();
        for (NodeIssue issue : this.issues(workflow)) {
            if (issue.kind() == NodeIssue.Kind.KEYBIND) {
                out.add(issue);
            }
        }
        return out;
    }

    /** ERROR or WARNING if the workflow has one (the more severe wins), null if it has at most infos. */
    public NodeIssue.@Nullable Severity worst(Workflow workflow) {
        return worst(this.issues(workflow));
    }

    public static NodeIssue.@Nullable Severity worst(List<NodeIssue> issues) {
        NodeIssue.Severity worst = null;
        for (NodeIssue issue : issues) {
            if (issue.severity() == NodeIssue.Severity.ERROR) {
                return NodeIssue.Severity.ERROR;
            }
            if (issue.severity() == NodeIssue.Severity.WARNING) {
                worst = NodeIssue.Severity.WARNING;
            }
        }
        return worst;
    }

}
