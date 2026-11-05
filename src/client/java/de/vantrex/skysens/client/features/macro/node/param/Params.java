package de.vantrex.skysens.client.features.macro.node.param;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Typed, defensive getters on a params object: a missing or wrongly typed value gives the default. */
public final class Params {

    private Params() {
    }

    public static String getString(JsonObject params, String key, String def) {
        final JsonElement e = params.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    public static int getInt(JsonObject params, String key, int def) {
        final JsonElement e = params.get(key);
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber()) {
            return e.getAsInt();
        }
        return def;
    }

    public static boolean getBool(JsonObject params, String key, boolean def) {
        final JsonElement e = params.get(key);
        if (e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean()) {
            return e.getAsBoolean();
        }
        return def;
    }

    /** The enum constant stored under the key, or def when missing or no longer a constant name. */
    public static <E extends Enum<E>> E getEnum(JsonObject params, String key, Class<E> type, E def) {
        final String name = getString(params, key, null);
        return name == null ? def : parseEnum(type, name, def);
    }

    public static <E extends Enum<E>> @Nullable E parseEnum(Class<E> type, String name, @Nullable E def) {
        for (E constant : type.getEnumConstants()) {
            if (constant.name().equals(name)) {
                return constant;
            }
        }
        return def;
    }

    public static List<String> getStringList(JsonObject params, String key) {
        final List<String> out = new ArrayList<>();
        final JsonElement e = params.get(key);
        if (e != null && e.isJsonArray()) {
            for (JsonElement item : e.getAsJsonArray()) {
                if (item.isJsonPrimitive()) {
                    out.add(item.getAsString());
                }
            }
        }
        return out;
    }

    public static void putStringList(JsonObject params, String key, List<String> values) {
        final JsonArray array = new JsonArray();
        values.forEach(v -> array.add(new JsonPrimitive(v)));
        params.add(key, array);
    }

    /** Stable text form of params (keys sorted), for comparing rules. */
    public static String canonical(JsonObject params) {
        final StringBuilder sb = new StringBuilder();
        params.keySet().stream().sorted().forEach(k -> sb.append(k).append('=').append(params.get(k)).append(';'));
        return sb.toString();
    }

}
