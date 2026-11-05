package de.vantrex.skysens.client.features.macro.engine;

import de.vantrex.skysens.client.SkysensClient;
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
import de.vantrex.skysens.client.util.ClientUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.InputWithModifiers;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/** Turns raw key/mouse events into macro runs and decides whether the event is consumed. */
public final class MacroEngine {

    private static final MacroEngine INSTANCE = new MacroEngine();

    /** At most one "skipped, state unknown" hint per this long. */
    private static final long UNKNOWN_HINT_THROTTLE_MS = 3000;

    private final InputTracker tracker = new InputTracker();
    private long lastUnknownHintAt;

    public static MacroEngine getInstance() {
        return INSTANCE;
    }

    /** Called every client tick (also off SkyBlock): resolves double tap windows and long holds. */
    public void tick() {
        this.tracker.tick(this::resolveDeferred);
    }

    /** action: 1 press, 0 release, 2 repeat. Cancels {@code ci} only when an action ran for this press. */
    public void onInput(InputDevice device, int code, InputWithModifiers event, int action, CallbackInfo ci) {
        if (action == 0) {
            this.tracker.release(device, code, this::resolveDeferred);
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
        final TriggerIndex.BindingGroup group = index.lookup(chord);
        if (group == null) {
            return;
        }
        final MacroGate.State state = MacroGate.check();
        if (state == MacroGate.State.BLOCKED) {
            ClientUtil.sendDebug("Macro key " + chord.displayName() + " ignored: gated");
            return;
        }
        final boolean gui = state == MacroGate.State.GUI;
        if (!group.isDeferred()) {
            // Only SINGLE triggers: resolved right now, and the key is swallowed when an action ran.
            if (this.run(chord, PressMode.SINGLE, applicable(group.single(), gui), gui)) {
                this.tracker.consume(device, code);
                ci.cancel();
            }
            return;
        }
        if (gui && applicable(group.single(), true).isEmpty() && applicable(group.doubleTap(), true).isEmpty()
                && applicable(group.longHold(), true).isEmpty()) {
            return;
        }
        // Double tap / long hold on this key: the press reaches vanilla and is resolved later; only the second press
        // of a double tap can be swallowed.
        if (this.tracker.press(group, device, code, this::resolveDeferred) == InputTracker.PressResult.DOUBLE_TAP
                && this.run(chord, PressMode.DOUBLE_TAP, applicable(group.doubleTap(), gui), gui)) {
            this.tracker.consume(device, code);
            ci.cancel();
        }
    }

    private static List<TriggerRef> applicable(List<TriggerRef> refs, boolean gui) {
        if (!gui) {
            return refs;
        }
        final List<TriggerRef> out = new ArrayList<>();
        for (TriggerRef ref : refs) {
            if (ref.inGuis()) {
                out.add(ref);
            }
        }
        return out;
    }

    /** A pending press reached its single press or long hold. The gate is checked again: a screen may have opened. */
    private void resolveDeferred(InputTracker.Pending pending, PressMode mode) {
        final MacroGate.State state = MacroGate.check();
        if (state == MacroGate.State.BLOCKED) {
            return;
        }
        final boolean gui = state == MacroGate.State.GUI;
        this.run(pending.group().chord(), mode, applicable(pending.group().forMode(mode), gui), gui);
    }

    /** Resolves, logs and warns. Returns whether an action ran. */
    private boolean run(InputChord chord, PressMode mode, List<TriggerRef> candidates, boolean gui) {
        if (candidates.isEmpty()) {
            return false;
        }
        final MacroContext ctx = new MacroContext(chord, mode, Minecraft.getInstance().gui.screen());
        final Resolution resolution = this.resolve(ctx, candidates);
        ExecutionLog.getInstance().add(new ExecutionRecord(System.currentTimeMillis(), chord, mode, gui, resolution));
        MatchWarnings.maybeWarn(chord, resolution);
        return resolution.ran();
    }

    private Resolution resolve(MacroContext ctx, List<TriggerRef> candidates) {
        final List<GraphWalker.PathResult> paths = new ArrayList<>();
        int chosen = -1;
        for (TriggerRef ref : candidates) {
            final GraphWalker.PathResult path = GraphWalker.walk(ref, ctx);
            paths.add(path);
            if (chosen < 0 && path.end() == GraphWalker.End.ACTION) {
                chosen = paths.size() - 1;
            }
        }
        if (chosen < 0) {
            for (GraphWalker.PathResult path : paths) {
                if (path.end() == GraphWalker.End.END_UNKNOWN) {
                    final String reason = path.unknownReason() == null ? "state not known yet" : path.unknownReason();
                    this.unknownHint(reason);
                    return new Resolution(paths, -1, Resolution.Outcome.NO_MATCH_UNKNOWN_STATE, reason);
                }
            }
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
        final ActionOutcome outcome = type.execute(node, ctx);
        if (outcome.isRan()) {
            CooldownTracker.getInstance().start(cooldownKey, Params.getInt(node.params, ActionParams.COOLDOWN_MS, 0));
            return new Resolution(paths, chosen, Resolution.Outcome.RAN, null);
        }
        ClientUtil.sendDebug("Macro \"" + workflowName + "\" failed: " + outcome.message());
        return new Resolution(paths, chosen, Resolution.Outcome.FAILED, outcome.message());
    }

    private void unknownHint(String reason) {
        final long now = System.currentTimeMillis();
        if (now - this.lastUnknownHintAt < UNKNOWN_HINT_THROTTLE_MS) {
            return;
        }
        this.lastUnknownHintAt = now;
        this.hint("Macro skipped: " + reason);
    }

    private void hint(String text) {
        if (!SkysensConfig.getInstance().macroCategory.showBlockedHint) {
            return;
        }
        final Component message = Component.literal(text).withStyle(ChatFormatting.RED);
        if (SkysensClient.getInstance().isOnSkyBlock()) {
            ClientUtil.sendSecondaryActionBar(message, 1500);
        } else {
            // Only reachable with the dev bypass: the secondary action bar is only drawn on SkyBlock.
            ClientUtil.sendActionBar(message);
        }
    }

    private static String formatSeconds(long ms) {
        return String.format("%.1fs", ms / 1000.0);
    }

}
