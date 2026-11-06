package de.vantrex.skysens.client.features.macro.template;

import de.vantrex.skysens.client.features.macro.editor.clipboard.MacroClipboard;
import de.vantrex.skysens.client.features.macro.model.MacroDocument;
import de.vantrex.skysens.client.features.macro.model.MacroJson;
import de.vantrex.skysens.client.features.macro.model.Workflow;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Ready-made macros shipped as JSON files in {@code assets/skysens/macro/templates/}. The files are listed here
 * explicitly (nothing is scanned). A template is inserted as a new, switched-off macro with new ids and no key bound.
 */
public final class MacroTemplates {

    private static final String DIRECTORY = "/assets/skysens/macro/templates/";

    /** One template: its file name, the menu title and a one-line description. */
    public record Entry(String file, String title, String description) {
    }

    public static final List<Entry> FILES = List.of(
            new Entry("menu_key.json", "Menu key", "One key: /storage, double tap /pets, hold /wardrobe"),
            new Entry("context_warp.json", "Context warp", "Warps depending on the island you are on"),
            new Entry("bazaar_outside_dungeons.json", "Bazaar outside dungeons", "/bz, but not while in a dungeon run"),
            new Entry("party_helper.json", "Party helper", "/p warp as leader, a ready message otherwise"),
            new Entry("quick_tour.json", "Quick tour", "Notes that explain how macros are evaluated")
    );

    private MacroTemplates() {
    }

    public static List<Entry> entries() {
        return FILES;
    }

    /** Reads the template's macro, or null if the file is missing or invalid. */
    public static @Nullable MacroDocument load(Entry entry) {
        try (InputStream in = MacroTemplates.class.getResourceAsStream(DIRECTORY + entry.file())) {
            if (in == null) {
                return null;
            }
            return MacroJson.read(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException | MacroJson.MacroFormatException e) {
            return null;
        }
    }

    /** A new, switched-off macro made from the template (new ids, a name that is unused in the list), or null. */
    public static @Nullable Workflow instantiate(Entry entry, List<Workflow> existing) {
        final MacroDocument doc = load(entry);
        if (doc == null) {
            return null;
        }
        final List<Workflow> workflows = MacroClipboard.workflowsFor(doc, existing);
        return workflows.isEmpty() ? null : workflows.get(0);
    }

}
