package de.vantrex.skysens.client.ui.model;

import de.vantrex.skysens.client.config.annotation.ConfigMasterToggle;
import io.github.notenoughupdates.moulconfig.ChromaColour;
import io.github.notenoughupdates.moulconfig.observer.Property;
import io.github.notenoughupdates.moulconfig.processor.ProcessedOption;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * A single config option wrapping a MoulConfig {@link ProcessedOption}.
 * Never cache values read from {@link #get()}; re-read them every frame.
 */
public final class OptionNode {

    /** Fresh instance per config class, the source of default values; {@link #UNAVAILABLE} marks a class that cannot be built. */
    private static final Map<Class<?>, Object> DEFAULT_INSTANCES = new HashMap<>();
    private static final Object UNAVAILABLE = new Object();
    private static final double FLOAT_EPSILON = 1e-6;

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

    /** Whether this boolean switches its container (category or accordion) on or off. */
    public boolean isMasterToggle() {
        return kind == OptionKind.TOGGLE && annotation(ConfigMasterToggle.class) != null;
    }

    /**
     * Whether a master toggle of this option's container, or of any enclosing accordion, is currently off.
     * The master toggle itself is not affected by its own container's switch.
     */
    public boolean isDimmed() {
        List<OptionNode> siblings = parent != null ? parent.children : category != null ? category.options() : List.of();
        for (OptionNode sibling : siblings) {
            if (sibling != this && sibling.isMasterToggle() && !Boolean.TRUE.equals(sibling.get())) {
                return true;
            }
        }
        return parent != null && parent.isDimmed();
    }

    /**
     * The value of the backing field on a fresh instance of its declaring class, if known. Empty for accordions,
     * transient fields (UI state) and {@code Property} fields, and when the class cannot be instantiated.
     */
    public Optional<Object> defaultValue() {
        if (field == null || kind == OptionKind.ACCORDION || Modifier.isTransient(field.getModifiers())
                || Property.class.isAssignableFrom(field.getType())) {
            return Optional.empty();
        }
        Object instance = DEFAULT_INSTANCES.computeIfAbsent(field.getDeclaringClass(), OptionNode::instantiate);
        if (instance == UNAVAILABLE) {
            return Optional.empty();
        }
        try {
            field.setAccessible(true);
            return Optional.ofNullable(field.get(instance));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return Optional.empty();
        }
    }

    /** Sets the default value if the current one differs. Returns whether the value changed. */
    public boolean resetToDefault() {
        Optional<Object> def = defaultValue();
        if (def.isEmpty() || isDefault()) {
            return false;
        }
        set(def.get());
        return true;
    }

    private static Object instantiate(Class<?> type) {
        try {
            var constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException | RuntimeException e) {
            return UNAVAILABLE;
        }
    }

    /** Whether the current value equals the default (also true when no default is known). */
    public boolean isDefault() {
        Optional<Object> def = defaultValue();
        return def.isEmpty() || sameValue(def.get(), get());
    }

    private static boolean sameValue(Object a, Object b) {
        if (a instanceof Float x && b instanceof Float y) {
            return Math.abs(x - y) <= FLOAT_EPSILON;
        }
        if (a instanceof Double x && b instanceof Double y) {
            return Math.abs(x - y) <= FLOAT_EPSILON;
        }
        if (a instanceof ChromaColour x && b instanceof ChromaColour y) {
            return x.toLegacyString().equals(y.toLegacyString());
        }
        return Objects.equals(a, b);
    }
}
