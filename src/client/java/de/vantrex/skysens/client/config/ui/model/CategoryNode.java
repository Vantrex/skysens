package de.vantrex.skysens.client.config.ui.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A config category (sidebar page). Subcategories are nested in {@link #children()}.
 */
public final class CategoryNode {

    private final String id;
    private final String name;
    private final String description;
    private CategoryNode parent;
    private final List<CategoryNode> children = new ArrayList<>();
    private final List<OptionNode> options = new ArrayList<>();

    public CategoryNode(String id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    /** Unique identifier of the category (as given by MoulConfig). */
    public String id() {
        return id;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    /** Parent category, or null for a top-level category. */
    public CategoryNode parent() {
        return parent;
    }

    void setParent(CategoryNode parent) {
        this.parent = parent;
    }

    /** Subcategories in declaration order. */
    public List<CategoryNode> children() {
        return children;
    }

    /** Top-level options of this category in order; accordions contain their own children. */
    public List<OptionNode> options() {
        return options;
    }

    @Override
    public String toString() {
        return name;
    }
}
