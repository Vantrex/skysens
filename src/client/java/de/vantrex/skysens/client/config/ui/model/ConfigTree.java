package de.vantrex.skysens.client.config.ui.model;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.annotation.ConfigEditorDynamicZoneDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorBoolean;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorColour;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorDropdown;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorSlider;
import io.github.notenoughupdates.moulconfig.annotations.ConfigEditorText;
import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorAccordion;
import io.github.notenoughupdates.moulconfig.processor.ProcessedCategory;
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the category/option tree from the MoulConfig processor. Build a fresh tree on every screen open.
 */
public final class ConfigTree {

    private ConfigTree() {
    }

    /** Builds the top-level categories (declaration order); subcategories are in {@link CategoryNode#children()}. */
    public static List<CategoryNode> build() {
        var processor = SkysensConfig.getConfig().getProcessor();
        Map<String, CategoryNode> byId = new HashMap<>();
        List<CategoryNode> roots = new ArrayList<>();

        for (ProcessedCategory pc : processor.getAllCategories().values()) {
            CategoryNode node = new CategoryNode(pc.getIdentifier(),
                    pc.getDisplayName().getText(), pc.getDescription().getText());
            byId.put(pc.getIdentifier(), node);
            String parentId = pc.getParentCategoryId();
            CategoryNode parent = parentId == null ? null : byId.get(parentId);
            if (parent != null) {
                node.setParent(parent);
                parent.children().add(node);
            } else {
                roots.add(node);
            }

            Map<Integer, OptionNode> accordionsById = new HashMap<>();
            for (ProcessedOption opt : pc.getOptions()) {
                Field field = opt instanceof ProcessedOption.HasField hf ? hf.getField() : null;
                OptionKind kind = kindOf(opt, field);
                OptionNode on = new OptionNode(opt, field, kind,
                        opt.getName().getText(), opt.getDescription().getText(), node);

                OptionNode container = opt.getAccordionId() == -1 ? null : accordionsById.get(opt.getAccordionId());
                if (container != null) {
                    on.setParent(container);
                    container.children().add(on);
                } else {
                    node.options().add(on);
                }
                if (opt.getEditor() instanceof GuiOptionEditorAccordion acc) {
                    accordionsById.put(acc.getAccordionId(), on);
                }
            }
        }
        return roots;
    }

    private static OptionKind kindOf(ProcessedOption opt, Field field) {
        if (opt.getEditor() instanceof GuiOptionEditorAccordion) {
            return OptionKind.ACCORDION;
        }
        if (field == null) {
            return OptionKind.UNSUPPORTED;
        }
        if (field.isAnnotationPresent(ConfigEditorBoolean.class)) return OptionKind.TOGGLE;
        if (field.isAnnotationPresent(ConfigEditorSlider.class)) return OptionKind.SLIDER;
        if (field.isAnnotationPresent(ConfigEditorDropdown.class)) return OptionKind.DROPDOWN;
        if (field.isAnnotationPresent(ConfigEditorDynamicZoneDropdown.class)) return OptionKind.ZONE_DROPDOWN;
        if (field.isAnnotationPresent(ConfigEditorColour.class)) return OptionKind.COLOUR;
        if (field.isAnnotationPresent(ConfigEditorText.class)) return OptionKind.TEXT;
        return OptionKind.UNSUPPORTED;
    }

    /** All non-accordion options of the given categories, depth-first (categories, subcategories, accordion children). */
    public static List<OptionNode> allLeaves(List<CategoryNode> categories) {
        List<OptionNode> out = new ArrayList<>();
        for (CategoryNode c : categories) {
            collect(c, out);
        }
        return out;
    }

    private static void collect(CategoryNode category, List<OptionNode> out) {
        collectOptions(category.options(), out);
        for (CategoryNode child : category.children()) {
            collect(child, out);
        }
    }

    private static void collectOptions(List<OptionNode> options, List<OptionNode> out) {
        for (OptionNode o : options) {
            if (o.kind() == OptionKind.ACCORDION) {
                collectOptions(o.children(), out);
            } else {
                out.add(o);
            }
        }
    }
}
