package de.vantrex.skysens.client.features.macro;

import de.vantrex.skysens.client.features.macro.model.MacroFormatUpgrader;
import com.google.gson.JsonObject;
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;
import de.vantrex.skysens.client.features.macro.analysis.GraphValidator;
import de.vantrex.skysens.client.features.macro.analysis.KeybindConflicts;
import de.vantrex.skysens.client.features.macro.analysis.ValidationResult;
import de.vantrex.skysens.client.skyblock.mayor.MayorService;
import de.vantrex.skysens.client.features.macro.analysis.NodeIssue;
import de.vantrex.skysens.client.features.macro.engine.MacroEngine;
import de.vantrex.skysens.client.util.ClientUtil;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import de.vantrex.skysens.client.features.macro.engine.TriggerIndex;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.MacroJson;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeType;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.persistence.RepositoryRegistry;
import lombok.Getter;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

/** Owns the parsed workflows and the trigger index. The repository holds the raw JSON they are parsed from. */
public final class MacroService {

    private static final MacroService INSTANCE = new MacroService();

    @Getter
    private List<Workflow> workflows = new ArrayList<>();
    @Getter
    private TriggerIndex triggerIndex = TriggerIndex.EMPTY;
    private final Map<String, Long> lastConflictAnnounced = new HashMap<>();
    /**
     * Computed lazily (null = stale): validation builds messages with key names, which come from GLFW, and the first
     * parse runs during mod init, before GLFW is initialised. Calling GLFW that early crashes the game on startup.
     */
    private @Nullable ValidationResult validation;
    private boolean usesMayor;

    public static MacroService getInstance() {
        return INSTANCE;
    }

    /** Parses the loaded repository. Call after RepositoryRegistry.loadRepositories(). */
    public void init() {
        this.parse();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MacroEngine.getInstance().tick();
            if (this.usesMayor) {
                MayorService.getInstance().refreshIfNeeded();
            }
        });
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
        this.announceConflicts();
    }

    private void parse() {
        final MacroRepository repository = RepositoryRegistry.MACRO_REPOSITORY;
        List<? extends JsonElement> raw = repository.workflows;
        if (repository.version < MacroJson.CURRENT_VERSION && raw != null) {
            // saved macros go through the same format upgrade as clipboard imports and templates
            final JsonObject root = new JsonObject();
            final JsonArray array = new JsonArray();
            raw.forEach(array::add);
            root.add("workflows", array);
            MacroFormatUpgrader.upgrade(root, repository.version);
            raw = root.getAsJsonArray("workflows").asList();
        }
        this.workflows = new ArrayList<>(MacroJson.parseWorkflows(raw));
        this.rebuildIndex();
        this.announceConflicts();
    }

    /** One chat line per keybind conflict, at most one per conflict every WARNING_THROTTLE_MS. Only while in a world. */
    private void announceConflicts() {
        if (Minecraft.getInstance().player == null) {
            return;
        }
        final long now = System.currentTimeMillis();
        for (NodeIssue issue : KeybindConflicts.compute(this.workflows)) {
            final Long last = this.lastConflictAnnounced.get(issue.message());
            if (last == null || now - last >= MacroConstants.WARNING_THROTTLE_MS) {
                this.lastConflictAnnounced.put(issue.message(), now);
                ClientUtil.sendPrefixed("§eMacros: " + issue.message());
            }
        }
    }

    private void rebuildIndex() {
        this.triggerIndex = TriggerIndex.build(this.workflows);
        this.validation = null;
        this.usesMayor = this.workflows.stream().anyMatch(wf -> wf.active && wf.nodes.stream().anyMatch(n -> n.params.toString().contains("\"type\":\"mayor\"")));
    }

    public int nodeCount() {
        return this.workflows.stream().mapToInt(wf -> wf.nodes.size()).sum();
    }

    public int unknownNodeCount() {
        return (int) this.workflows.stream().flatMap(wf -> wf.nodes.stream())
                .filter(node -> !NodeTypes.isKnown(node.type)).count();
    }

    /** All issues of a workflow from the last parse/commit (validation, overlaps, keybind conflicts). */
    public List<NodeIssue> issues(Workflow workflow) {
        return this.validation().issues(workflow);
    }

    public ValidationResult validation() {
        if (this.validation == null) {
            this.validation = GraphValidator.validate(this.workflows);
        }
        return this.validation;
    }

}
