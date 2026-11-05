package de.vantrex.skysens.client.features.macro.engine;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.macro.MacroService;
import de.vantrex.skysens.client.features.macro.action.ActionNodeType;
import de.vantrex.skysens.client.features.macro.action.ActionOutcome;
import de.vantrex.skysens.client.features.macro.action.ActionParams;
import de.vantrex.skysens.client.features.macro.action.CloseScreenStep;
import de.vantrex.skysens.client.features.macro.action.CooldownTracker;
import de.vantrex.skysens.client.features.macro.action.MacroSender;
import de.vantrex.skysens.client.features.macro.facts.MacroContext;
import de.vantrex.skysens.client.features.macro.model.MacroNode;
import de.vantrex.skysens.client.features.macro.node.param.Params;
import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.util.ClientUtil;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Turns raw key/mouse events into macro runs and decides whether the event is consumed. */
public final class MacroEngine {

    private static final MacroEngine INSTANCE = new MacroEngine();

    private final InputTracker tracker = new InputTracker();

    public static MacroEngine getInstance() {
        return INSTANCE;
    }

    /** action: 1 press, 0 release, 2 repeat. Cancels {@code ci} only when an action ran for this press. */
    public void onInput(InputDevice device, int code, InputWithModifiers event, int action, CallbackInfo ci) {
        if (action == 0) {
            this.tracker.release(device, code);
            return;
        }
        if (action == 2) {
            if (this.tracker.isConsumed(device, code)) {
                ci.cancel();
            }
            return;
        }
        if (action != 1 || code < 0) {
            return;
        }
        final TriggerIndex index = MacroService.getInstance().getTriggerIndex();
        if (index.isEmpty()) {
            return;
        }
        final InputChord chord = InputChord.of(device, code, event);
        final List<TriggerRef> refs = index.lookup(chord);
        if (refs.isEmpty()) {
            return;
        }
        final MacroGate.State state = MacroGate.check();
        if (state == MacroGate.State.BLOCKED) {
            ClientUtil.sendDebug("Macro key " + chord.displayName() + " ignored: gated");
            return;
        }
        final boolean gui = state == MacroGate.State.GUI;
        final List<TriggerRef> candidates = new ArrayList<>();
        for (TriggerRef ref : refs) {
            if (!gui || ref.inGuis()) {
                candidates.add(ref);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }
        final Resolution resolution = this.resolve(chord, candidates, gui);
        ExecutionLog.getInstance().add(new ExecutionRecord(System.currentTimeMillis(), chord, PressMode.SINGLE, gui, resolution));
        if (resolution.ran()) {
            this.tracker.consume(device, code);
            ci.cancel();
        }
    }

    private Resolution resolve(InputChord chord, List<TriggerRef> candidates, boolean gui) {
        final List<GraphWalker.PathResult> paths = new ArrayList<>();
        int chosen = -1;
        for (TriggerRef ref : candidates) {
            final GraphWalker.PathResult path = GraphWalker.walk(ref);
            paths.add(path);
            if (chosen < 0 && path.end() == GraphWalker.End.ACTION) {
                chosen = paths.size() - 1;
            }
        }
        if (chosen < 0) {
            return new Resolution(paths, -1, Resolution.Outcome.NO_ACTION, "no path reached an action");
        }
        final GraphWalker.PathResult path = paths.get(chosen);
        final MacroNode node = path.action();
        final ActionNodeType type = path.actionType();
        final String workflowName = path.ref().workflow().name;
        final String cooldownKey = CooldownTracker.key(path.ref().workflow().id, node.id);

        // The first match is final: when it is blocked the press is dropped (it passes through), the next one is not tried.
        final long cooldown = CooldownTracker.getInstance().remaining(cooldownKey);
        if (cooldown > 0) {
            final ActionOutcome outcome = ActionOutcome.onCooldown(cooldown);
            this.hint("\"" + workflowName + "\" is on cooldown (" + formatSeconds(cooldown) + ")");
            return new Resolution(paths, chosen, Resolution.Outcome.ON_COOLDOWN, outcome.message());
        }
        if (type.sendsToServer()) {
            final long rateLimit = MacroSender.rateLimitRemaining();
            if (rateLimit > 0) {
                final ActionOutcome outcome = ActionOutcome.rateLimited(rateLimit);
                this.hint("Macros are rate limited (" + formatSeconds(rateLimit) + ")");
                return new Resolution(paths, chosen, Resolution.Outcome.RATE_LIMITED, outcome.message());
            }
        }

        if (Params.getBool(node.params, ActionParams.CLOSE_SCREEN_FIRST, false)) {
            CloseScreenStep.run();
        }
        final ActionOutcome outcome = type.execute(node, new MacroContext(chord, PressMode.SINGLE));
        if (outcome.isRan()) {
            CooldownTracker.getInstance().start(cooldownKey, Params.getInt(node.params, ActionParams.COOLDOWN_MS, 0));
            return new Resolution(paths, chosen, Resolution.Outcome.RAN, null);
        }
        ClientUtil.sendDebug("Macro \"" + workflowName + "\" failed: " + outcome.message());
        return new Resolution(paths, chosen, Resolution.Outcome.FAILED, outcome.message());
    }

    private void hint(String text) {
        if (!SkysensConfig.getInstance().macroCategory.showBlockedHint) {
            return;
        }
        if (SkysensClient.getInstance().isOnSkyBlock()) {
            ClientUtil.sendSecondaryActionBar(Component.literal(text), 1500);
        } else {
            // Only reachable with the dev bypass: the secondary action bar is only drawn on SkyBlock.
            ClientUtil.sendActionBar(Component.literal(text));
        }
    }

    private static String formatSeconds(long ms) {
        return String.format("%.1fs", ms / 1000.0);
    }

}
