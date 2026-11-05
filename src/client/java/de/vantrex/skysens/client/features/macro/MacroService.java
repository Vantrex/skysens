package de.vantrex.skysens.client.features.macro;

import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.engine.TriggerIndex;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.MacroJson;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.persistence.RepositoryRegistry;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Owns the parsed workflows and the trigger index. The repository holds the raw JSON they are parsed from. */
public final class MacroService {

    private static final MacroService INSTANCE = new MacroService();

    @Getter
    private List<Workflow> workflows = new ArrayList<>();
    @Getter
    private TriggerIndex triggerIndex = TriggerIndex.EMPTY;

    public static MacroService getInstance() {
        return INSTANCE;
    }

    /** Parses the loaded repository. Call after RepositoryRegistry.loadRepositories(). */
    public void init() {
        this.parse();
    }

    /** Re-reads the repository file from disk (discarding unsaved in-memory changes) and parses it again. */
    public void reload() {
        RepositoryRegistry.reloadMacroRepository();
        this.parse();
    }

    /** Replaces the workflows, writes them into the repository and saves it. */
    public void commit(List<Workflow> newWorkflows) {
        this.workflows = new ArrayList<>(newWorkflows);
        final MacroRepository repository = RepositoryRegistry.MACRO_REPOSITORY;
        repository.version = MacroJson.CURRENT_VERSION;
        repository.workflows = MacroJson.workflowsToRaw(this.workflows);
        RepositoryRegistry.saveRepository(repository);
        this.rebuildIndex();
    }

    private void parse() {
        this.workflows = new ArrayList<>(MacroJson.parseWorkflows(RepositoryRegistry.MACRO_REPOSITORY.workflows));
        this.rebuildIndex();
    }

    private void rebuildIndex() {
        this.triggerIndex = TriggerIndex.build(this.workflows);
    }

    public int nodeCount() {
        return this.workflows.stream().mapToInt(wf -> wf.nodes.size()).sum();
    }

    public int unknownNodeCount() {
        return (int) this.workflows.stream().flatMap(wf -> wf.nodes.stream())
                .filter(node -> !NodeTypes.isKnown(node.type)).count();
    }

    /** All issues of a workflow: node validation plus wires pointing at nodes that do not exist. */
    public List<NodeIssue> issues(Workflow workflow) {
        final List<NodeIssue> out = new ArrayList<>();
        final Set<String> ids = new HashSet<>();
        for (MacroNode node : workflow.nodes) {
            ids.add(node.id);
            final NodeType type = NodeTypes.resolve(node.type);
            type.validate(node, out);
        }
        for (Connection connection : workflow.connections) {
            if (!ids.contains(connection.from) || !ids.contains(connection.to)) {
                out.add(new NodeIssue(NodeIssue.Severity.WARNING, connection.from, "Wire " + connection.from + " to " + connection.to + " points at a missing node"));
            }
        }
        return out;
    }

}
