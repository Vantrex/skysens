package de.vantrex.skysens.client.ui.model;

import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A single config option wrapping a MoulConfig {@link ProcessedOption}.
 * Never cache values read from {@link #get()}; re-read them every frame.
 */
public final class OptionNode {

    private final ProcessedOption option;
    private final Field field;
    private final OptionKind kind;
    private final String name;
    private final String description;
    private final CategoryNode category;
    private OptionNode parent;
    private final List<OptionNode> children = new ArrayList<>();

    public OptionNode(ProcessedOption option, Field field, OptionKind kind, String name, String description,
                      CategoryNode category) {
        this.option = option;
        this.field = field;
        this.kind = kind;
        this.name = name == null ? "" : name;
        this.description = description == null ? "" : description;
        this.category = category;
    }

    public ProcessedOption option() {
        return option;
    }

    /** Backing field, may be null. */
    public Field field() {
        return field;
    }

    public OptionKind kind() {
        return kind;
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public CategoryNode category() {
        return category;
    }

    /** Enclosing accordion option, or null when the option is directly in its category. */
    public OptionNode parent() {
        return parent;
    }

    void setParent(OptionNode parent) {
        this.parent = parent;
    }

    /** Child options; only non-empty for {@link OptionKind#ACCORDION}. */
    public List<OptionNode> children() {
        return children;
    }

    /**
     * The value class of the option: the generic argument for {@code Property<T>}, otherwise the field type.
     * May be a primitive class (e.g. {@code float.class}).
     */
    public Class<?> valueType() {
        Type t = option.getType();
        if (t instanceof Class<?> c) {
            return c;
        }
        if (t instanceof ParameterizedType p && p.getRawType() instanceof Class<?> c) {
            return c;
        }
        return field != null ? field.getType() : Object.class;
    }

    /** Current value (Property unwrapped). */
    public Object get() {
        return option.get();
    }

    /**
     * Sets the value. The caller must pass the exact boxed type of {@link #valueType()}.
     * Only call when the value actually changed.
     */
    public boolean set(Object value) {
        return option.set(value);
    }

    /** Annotation of the backing field, or null. */
    public <A extends Annotation> A annotation(Class<A> type) {
        return field == null ? null : field.getAnnotation(type);
    }

    /** Breadcrumb of containers excluding this option, e.g. "Mining › Mining Notifications". */
    public String breadcrumb() {
        List<String> parts = new ArrayList<>();
        for (OptionNode p = parent; p != null; p = p.parent) {
            parts.add(0, p.name);
        }
        if (category != null) {
            parts.add(0, category.name());
            for (CategoryNode c = category.parent(); c != null; c = c.parent()) {
                parts.add(0, c.name());
            }
        }
        return String.join(" › ", parts);
    }

    /** Whether name or description contains the (already lower-cased) query. */
    public boolean matches(String lowerQuery) {
        if (lowerQuery == null || lowerQuery.isEmpty()) {
            return true;
        }
        return name.toLowerCase(Locale.ROOT).contains(lowerQuery)
                || description.toLowerCase(Locale.ROOT).contains(lowerQuery);
    }
}
