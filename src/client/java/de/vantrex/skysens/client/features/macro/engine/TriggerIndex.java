package de.vantrex.skysens.client.features.macro.engine;

import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.features.macro.node.trigger.HotkeyTriggerNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps key chords to the triggers bound to them, in evaluation order: workflow order, then the trigger's position on
 * the canvas (top to bottom, then left to right). Only active workflows and enabled SINGLE triggers are indexed.
 */
public final class TriggerIndex {

    public static final TriggerIndex EMPTY = new TriggerIndex(Map.of(), 0);

    private final Map<InputChord, List<TriggerRef>> byChord;
    private final int size;

    private TriggerIndex(Map<InputChord, List<TriggerRef>> byChord, int size) {
        this.byChord = byChord;
        this.size = size;
    }

    public static TriggerIndex build(List<Workflow> workflows) {
        final Map<InputChord, List<TriggerRef>> map = new HashMap<>();
        int count = 0;
        for (int i = 0; i < workflows.size(); i++) {
            final Workflow workflow = workflows.get(i);
            if (!workflow.active) {
                continue;
            }
            final List<MacroNode> triggers = new ArrayList<>();
            for (MacroNode node : workflow.nodes) {
                if (!node.disabled && NodeTypes.resolve(node.type) instanceof HotkeyTriggerNode) {
                    triggers.add(node);
                }
            }
            triggers.sort(Comparator.<MacroNode>comparingInt(n -> n.y).thenComparingInt(n -> n.x));
            for (MacroNode node : triggers) {
                final HotkeyTriggerNode.Spec spec = HotkeyTriggerNode.spec(node);
                // Double tap and long hold are not implemented yet: such triggers are not indexed.
                if (spec == null || spec.mode() != PressMode.SINGLE) {
                    continue;
                }
                map.computeIfAbsent(spec.chord(), k -> new ArrayList<>())
                        .add(new TriggerRef(i, workflow, node, spec.chord(), spec.mode(), spec.inGuis()));
                count++;
            }
        }
        return new TriggerIndex(map, count);
    }

    /** All triggers on the chord in evaluation order; empty when none. */
    public List<TriggerRef> lookup(InputChord chord) {
        return this.byChord.getOrDefault(chord, List.of());
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public int size() {
        return this.size;
    }

}
