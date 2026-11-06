package de.vantrex.skysens.client.features.macro.engine;

import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import de.vantrex.skysens.client.features.macro.node.NodeTypes;
import de.vantrex.skysens.client.features.macro.node.trigger.HotkeyTriggerNode;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Maps key chords to the triggers bound to them, in evaluation order: workflow order, then the trigger's position on
 * the canvas (top to bottom, then left to right). Only active workflows and enabled triggers are indexed. All triggers
 * of one chord form a {@link BindingGroup}; its mix of press modes decides when a press is resolved.
 */
public final class TriggerIndex {

    public static final TriggerIndex EMPTY = new TriggerIndex(Map.of(), 0);

    /** Every trigger on one chord, split by press mode (each list in evaluation order). */
    public record BindingGroup(InputChord chord, List<TriggerRef> single, List<TriggerRef> doubleTap, List<TriggerRef> longHold) {

        public boolean hasDoubleTap() {
            return !this.doubleTap.isEmpty();
        }

        public boolean hasLongHold() {
            return !this.longHold.isEmpty();
        }

        /** Whether presses of this chord are resolved later than the press itself (double tap or long hold present). */
        public boolean isDeferred() {
            return this.hasDoubleTap() || this.hasLongHold();
        }

        public List<TriggerRef> forMode(PressMode mode) {
            return switch (mode) {
                case SINGLE -> this.single;
                case DOUBLE_TAP -> this.doubleTap;
                case LONG_HOLD -> this.longHold;
            };
        }
    }

    private final Map<InputChord, BindingGroup> byChord;
    private final int size;

    private TriggerIndex(Map<InputChord, BindingGroup> byChord, int size) {
        this.byChord = byChord;
        this.size = size;
    }

    public static TriggerIndex build(List<Workflow> workflows) {
        final Map<InputChord, BindingGroup> map = new HashMap<>();
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
                if (spec == null) {
                    continue;
                }
                final BindingGroup group = map.computeIfAbsent(spec.chord(),
                        k -> new BindingGroup(k, new ArrayList<>(), new ArrayList<>(), new ArrayList<>()));
                group.forMode(spec.mode()).add(new TriggerRef(i, workflow, node, spec.chord(), spec.mode(), spec.inGuis()));
                count++;
            }
        }
        return new TriggerIndex(map, count);
    }

    /** Every binding group, in no particular order. */
    public java.util.Collection<BindingGroup> groups() {
        return this.byChord.values();
    }

    /** The group bound to the chord, null when none. */
    public @Nullable BindingGroup lookup(InputChord chord) {
        return this.byChord.get(chord);
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    public int size() {
        return this.size;
    }

}
