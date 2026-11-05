package de.vantrex.skysens.client.features.macro.action;

import com.google.gson.JsonPrimitive;
import de.vantrex.skysens.client.features.macro.node.param.ParamKind;
import de.vantrex.skysens.client.features.macro.node.param.ParamSpec;

import java.util.ArrayList;
import java.util.List;

/** Params every action node has, plus a helper to build the full list. */
public final class ActionParams {

    public static final String CLOSE_SCREEN_FIRST = "closeScreenFirst";
    public static final String COOLDOWN_MS = "cooldownMs";

    private ActionParams() {
    }

    /** The node specific specs followed by the common ones. */
    public static List<ParamSpec> with(ParamSpec... own) {
        final List<ParamSpec> out = new ArrayList<>(List.of(own));
        out.add(ParamSpec.of(CLOSE_SCREEN_FIRST, "Close inventory first", "Close the open inventory or screen before the action runs", ParamKind.BOOL, new JsonPrimitive(false)));
        out.add(ParamSpec.of(COOLDOWN_MS, "Cooldown (ms)", "Minimum time between two runs of this action, 0 for none", ParamKind.INT, new JsonPrimitive(0)));
        return out;
    }

    public static String shorten(String text) {
        return text.length() > 24 ? text.substring(0, 23) + "…" : text;
    }

}
