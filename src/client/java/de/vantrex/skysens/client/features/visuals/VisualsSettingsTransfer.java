package de.vantrex.skysens.client.features.visuals;

import de.vantrex.skysens.client.config.SkysensConfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.vantrex.skysens.client.features.visuals.config.SwingAnimationAccordion;
import de.vantrex.skysens.client.features.visuals.config.VisualsCategory;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.experimental.UtilityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Exports and imports the visuals and swing animation settings as a copyable string
 * of the form {@code SKS1:<base64(gzip(json))>}.
 */
@UtilityClass
public class VisualsSettingsTransfer {

    private final String PREFIX = "SKS1:";
    private final int VERSION = 1;
    private final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public int export() {
        final SkysensConfig config = SkysensConfig.getInstance();
        final JsonObject root = new JsonObject();
        root.addProperty("v", VERSION);
        root.add("visuals", GSON.toJsonTree(config.visualsCategory));

        final String code;
        try {
            code = PREFIX + Base64.getEncoder().encodeToString(gzip(GSON.toJson(root).getBytes(StandardCharsets.UTF_8)));
        } catch (IOException e) {
            ClientUtil.sendMessage("§cFailed to export settings");
            return 0;
        }

        Minecraft.getInstance().keyboardHandler.setClipboard(code);
        final Component copyAgain = Component.literal("[Copy again]").setStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent.CopyToClipboard(code))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Click to copy the settings string again"))));
        ClientUtil.sendMessage(Component.literal("§aCopied settings to clipboard ").append(copyAgain));
        return 1;
    }

    public int importFrom(String data) {
        try {
            final String trimmed = data == null ? "" : data.trim();
            if (!trimmed.startsWith(PREFIX)) {
                throw new IllegalArgumentException("Missing prefix");
            }
            final byte[] json = gunzip(Base64.getDecoder().decode(trimmed.substring(PREFIX.length())));
            final JsonObject root = GSON.fromJson(new String(json, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null || !root.has("v") || root.get("v").getAsInt() != VERSION) {
                throw new IllegalArgumentException("Unsupported version");
            }
            final VisualsCategory visuals = GSON.fromJson(requireElement(root, "visuals"), VisualsCategory.class);
            // Older strings carry the swing settings under a separate "swing" key.
            final JsonElement legacySwing = root.get("swing");
            if (legacySwing != null && legacySwing.isJsonObject()) {
                visuals.swingAnimation = GSON.fromJson(legacySwing, SwingAnimationAccordion.class);
            }
            visuals.clamp();

            // Copy into the existing objects: features hold references to them.
            final SkysensConfig config = SkysensConfig.getInstance();
            config.visualsCategory.copyFrom(visuals);
            SkysensConfig.getInstance().saveNow();
            ClientUtil.sendMessage("§aImported visuals and swing settings");
            return 1;
        } catch (Exception e) {
            ClientUtil.sendMessage("§cInvalid settings string");
            return 0;
        }
    }

    public int reset() {
        final SkysensConfig config = SkysensConfig.getInstance();
        config.visualsCategory.copyFrom(new VisualsCategory());
        SkysensConfig.getInstance().saveNow();
        ClientUtil.sendMessage("§aReset visuals and swing settings");
        return 1;
    }

    private JsonElement requireElement(JsonObject root, String key) {
        final JsonElement element = root.get(key);
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("Missing " + key);
        }
        return element;
    }

    private byte[] gzip(byte[] input) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gz = new GZIPOutputStream(out)) {
            gz.write(input);
        }
        return out.toByteArray();
    }

    private byte[] gunzip(byte[] input) throws IOException {
        try (GZIPInputStream gz = new GZIPInputStream(new ByteArrayInputStream(input))) {
            return gz.readAllBytes();
        }
    }

}
