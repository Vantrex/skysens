package de.vantrex.skysens.client.features.macro.node.param;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;

/** Describes one parameter of a node or condition; drives the details panel and the default params. */
public record ParamSpec(String key, String label, String description, ParamKind kind,
                        @Nullable JsonElement defaultValue, @Nullable Supplier<List<Choice>> choices,
                        @Nullable Predicate<JsonObject> visibleIf) {

    public static ParamSpec of(String key, String label, String description, ParamKind kind, @Nullable JsonElement defaultValue) {
        return new ParamSpec(key, label, description, kind, defaultValue, null, null);
    }

    /** A params object holding every spec default value. */
    public static JsonObject defaults(List<ParamSpec> specs) {
        final JsonObject out = new JsonObject();
        for (ParamSpec spec : specs) {
            if (spec.defaultValue != null) {
                out.add(spec.key, spec.defaultValue.deepCopy());
            }
        }
        return out;
    }

}
