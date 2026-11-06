package de.vantrex.skysens.client.features.macro.engine;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.macro.MacroConstants;
import de.vantrex.skysens.client.features.macro.action.MacroToasts;
import de.vantrex.skysens.client.features.macro.config.MacroCategory;
import de.vantrex.skysens.client.features.macro.config.MatchWarningDisplay;
import de.vantrex.skysens.client.notification.Notification;
import de.vantrex.skysens.client.util.ClientUtil;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Warns when one press matched several branches. Only the first one runs (workflow order, then the first matching
 * rule of a Switch); the message lists everything that matched. Throttled per key.
 */
public final class MatchWarnings {

    private static final Map<InputChord, Long> LAST_WARNED = new HashMap<>();

    private MatchWarnings() {
    }

    public static void maybeWarn(InputChord chord, Resolution resolution) {
        final MacroCategory config = SkysensConfig.getInstance().macroCategory;
        if (config.multipleMatchWarning == MatchWarningDisplay.OFF || resolution.chosen() < 0) {
            return;
        }
        final List<String> also = new ArrayList<>();
        final GraphWalker.PathResult chosen = resolution.chosenPath();
        for (int i = 0; i < resolution.paths().size(); i++) {
            final GraphWalker.PathResult path = resolution.paths().get(i);
            if (i != resolution.chosen() && path.end() == GraphWalker.End.ACTION) {
                also.add("\"" + path.describe() + "\"");
            }
            for (GraphWalker.LogicStep step : path.steps()) {
                if (step.hasSeveralMatches() && (i == resolution.chosen() || path.end() == GraphWalker.End.ACTION)) {
                    final List<String> ports = step.matchedPorts();
                    also.add(step.nodeName() + " " + path.ref().workflow().name + ": " + String.join(", ", ports.subList(1, ports.size()))
                            .replace("rule:", "rule "));
                }
            }
        }
        if (also.isEmpty()) {
            return;
        }
        final long now = System.currentTimeMillis();
        final Long last = LAST_WARNED.get(chord);
        if (last != null && now - last < MacroConstants.WARNING_THROTTLE_MS) {
            return;
        }
        LAST_WARNED.put(chord, now);
        final String verb = resolution.ran() ? "ran" : "picked";
        final String text = chord.displayName() + ": " + verb + " \"" + chosen.describe() + "\"; also matched " + String.join(", ", also);
        if (config.multipleMatchWarning == MatchWarningDisplay.TOAST) {
            MacroToasts.show(Notification.builder()
                    .text(Component.literal("Macros: several matches on " + chord.displayName()))
                    .subtitle(Component.literal(text))
                    .level(config.multipleMatchWarningLevel)
                    .build());
        } else {
            ClientUtil.sendPrefixed("§eMacros: " + text);
        }
    }

}
