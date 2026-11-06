package de.vantrex.skysens.client.features.macro.editor;

import de.vantrex.skysens.client.features.macro.MacroService;
import de.vantrex.skysens.client.features.macro.engine.ExecutionLog;
import de.vantrex.skysens.client.features.macro.engine.ExecutionRecord;
import de.vantrex.skysens.client.features.macro.engine.GraphWalker;
import de.vantrex.skysens.client.features.macro.model.Connection;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** What the editor screen currently shows: the workflows, the selected one, the tab, the node selection and a pinned execution. */
public final class EditorState {

    public enum Tab { EDITOR, EXECUTIONS }

    /** How long the highlight of the last press takes to fade out. */
    public static final long HIGHLIGHT_FADE_MS = 15_000L;

    private final List<Workflow> workflows;
    private @Nullable Workflow current;
    private Tab tab = Tab.EDITOR;
    private final Set<String> selectedNodes = new LinkedHashSet<>();
    private @Nullable ExecutionRecord pinned;
    private boolean viewportDirty;

    public EditorState() {
        this.workflows = MacroService.getInstance().getWorkflows();
        this.current = this.workflows.isEmpty() ? null : this.workflows.get(0);
    }

    public List<Workflow> workflows() {
        return this.workflows;
    }

    public @Nullable Workflow current() {
        return this.current;
    }

    /** Switches the workflow (clears the node selection). Returns false if it already was the current one. */
    public boolean select(@Nullable Workflow workflow) {
        if (workflow == this.current) {
            return false;
        }
        this.current = workflow;
        this.selectedNodes.clear();
        return true;
    }

    public Tab tab() {
        return this.tab;
    }

    public void setTab(Tab tab) {
        this.tab = tab;
    }

    public Set<String> selectedNodes() {
        return this.selectedNodes;
    }

    public @Nullable ExecutionRecord pinned() {
        return this.pinned;
    }

    public void setPinned(@Nullable ExecutionRecord record) {
        this.pinned = record;
    }

    public boolean viewportDirty() {
        return this.viewportDirty;
    }

    public void markViewportDirty() {
        this.viewportDirty = true;
    }

    public void clearViewportDirty() {
        this.viewportDirty = false;
    }

    /**
     * The "last run" highlight for a workflow: the pinned execution (full strength) or else the newest one, fading out
     * over {@link #HIGHLIGHT_FADE_MS}. The chosen path is green, other paths that ended in an action amber.
     */
    public Highlight highlightFor(Workflow workflow, long now) {
        ExecutionRecord record = this.pinned;
        float strength = 1f;
        if (record == null) {
            final List<ExecutionRecord> recent = ExecutionLog.getInstance().recent(1);
            if (recent.isEmpty()) {
                return Highlight.NONE;
            }
            record = recent.get(0);
            strength = 1f - (float) (now - record.time()) / HIGHLIGHT_FADE_MS;
            if (strength <= 0f) {
                return Highlight.NONE;
            }
        }
        final Highlight.Builder builder = Highlight.builder(strength);
        final GraphWalker.PathResult chosen = record.resolution().chosenPath();
        for (GraphWalker.PathResult path : record.resolution().paths()) {
            if (!path.ref().workflow().id.equals(workflow.id)) {
                continue;
            }
            final Highlight.Kind kind = path == chosen ? Highlight.Kind.CHOSEN
                    : path.end() == GraphWalker.End.ACTION ? Highlight.Kind.ALSO : null;
            if (kind != null) {
                addPath(builder, workflow, path, kind);
            }
        }
        return builder.build();
    }

    private static void addPath(Highlight.Builder builder, Workflow workflow, GraphWalker.PathResult path, Highlight.Kind kind) {
        final List<String> ids = path.nodePath();
        for (int i = 0; i < ids.size(); i++) {
            builder.node(ids.get(i), kind);
            if (i == 0) {
                continue;
            }
            final String port = portTaken(path, ids.get(i - 1));
            Connection match = null;
            for (Connection connection : workflow.connections) {
                if (ids.get(i - 1).equals(connection.from) && ids.get(i).equals(connection.to)
                        && (match == null || connection.port.equals(port))) {
                    match = connection;
                }
            }
            if (match != null) {
                builder.wire(match, kind);
            }
        }
    }

    /** The output port a logic node routed to, "main" for triggers. */
    private static String portTaken(GraphWalker.PathResult path, String nodeId) {
        for (GraphWalker.LogicStep step : path.steps()) {
            if (step.nodeId().equals(nodeId) && step.port() != null) {
                return step.port();
            }
        }
        return "main";
    }

}
