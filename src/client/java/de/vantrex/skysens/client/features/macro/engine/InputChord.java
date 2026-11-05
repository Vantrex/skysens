package de.vantrex.skysens.client.features.macro.engine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.input.InputWithModifiers;
import org.jetbrains.annotations.Nullable;

/**
 * A key or mouse button plus the exact set of held modifiers (Ctrl, Shift, Alt as a bit mask). Persisted in trigger
 * params as {@code {"device":"KEYBOARD","code":75,"mods":["CTRL"]}} with GLFW codes.
 */
public record InputChord(InputDevice device, int code, int mods) {

    public static final int CTRL = 1;
    public static final int SHIFT = 2;
    public static final int ALT = 4;

    private static final String[] MOD_NAMES = {"CTRL", "SHIFT", "ALT"};

    /** The chord of a live event. The key's own modifier bit is stripped, so Left Alt alone is just "Left Alt". */
    public static InputChord of(InputDevice device, int code, InputWithModifiers event) {
        int mods = 0;
        if (event.hasControlDown()) {
            mods |= CTRL;
        }
        if (event.hasShiftDown()) {
            mods |= SHIFT;
        }
        if (event.hasAltDown()) {
            mods |= ALT;
        }
        if (device == InputDevice.KEYBOARD) {
            mods &= ~ownModifier(code);
        }
        return new InputChord(device, code, mods);
    }

    private static int ownModifier(int glfwKey) {
        return switch (glfwKey) {
            case 341, 345 -> CTRL;
            case 340, 344 -> SHIFT;
            case 342, 346 -> ALT;
            default -> 0;
        };
    }

    /** Parses the persisted form, or null when it is missing, malformed or unbound (code below 0). */
    public static @Nullable InputChord fromJson(@Nullable JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            return null;
        }
        final JsonObject o = json.getAsJsonObject();
        final JsonElement deviceJson = o.get("device");
        final JsonElement codeJson = o.get("code");
        if (deviceJson == null || !deviceJson.isJsonPrimitive() || codeJson == null || !codeJson.isJsonPrimitive()
                || !codeJson.getAsJsonPrimitive().isNumber()) {
            return null;
        }
        InputDevice device = null;
        for (InputDevice d : InputDevice.values()) {
            if (d.name().equals(deviceJson.getAsString())) {
                device = d;
            }
        }
        final int code = codeJson.getAsInt();
        if (device == null || code < 0) {
            return null;
        }
        int mods = 0;
        final JsonElement modsJson = o.get("mods");
        if (modsJson != null && modsJson.isJsonArray()) {
            for (JsonElement m : modsJson.getAsJsonArray()) {
                if (!m.isJsonPrimitive()) {
                    continue;
                }
                for (int i = 0; i < MOD_NAMES.length; i++) {
                    if (MOD_NAMES[i].equals(m.getAsString())) {
                        mods |= 1 << i;
                    }
                }
            }
        }
        return new InputChord(device, code, mods);
    }

    public JsonObject toJson() {
        final JsonObject o = new JsonObject();
        o.addProperty("device", this.device.name());
        o.addProperty("code", this.code);
        final JsonArray array = new JsonArray();
        for (int i = 0; i < MOD_NAMES.length; i++) {
            if ((this.mods & (1 << i)) != 0) {
                array.add(new JsonPrimitive(MOD_NAMES[i]));
            }
        }
        o.add("mods", array);
        return o;
    }

    public boolean isPrimaryMouseButton() {
        return this.device == InputDevice.MOUSE && (this.code == 0 || this.code == 1);
    }

    public String displayName() {
        final StringBuilder sb = new StringBuilder();
        if ((this.mods & CTRL) != 0) {
            sb.append("Ctrl+");
        }
        if ((this.mods & SHIFT) != 0) {
            sb.append("Shift+");
        }
        if ((this.mods & ALT) != 0) {
            sb.append("Alt+");
        }
        if (this.device == InputDevice.MOUSE) {
            sb.append("Mouse ").append(this.code + 1);
        } else {
            sb.append(InputConstants.Type.KEYSYM.getOrCreate(this.code).getDisplayName().getString());
        }
        return sb.toString();
    }

}
