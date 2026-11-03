package de.vantrex.skysens.client.integration;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Reflective access to a Kotlin {@code object} of another mod, so we don't need a compile time dependency.
 * Any failure (renamed class, changed signature) is logged once and turns the accessor off.
 */
public final class KotlinObjectAccessor {

    private static final Logger LOGGER = LoggerFactory.getLogger("skysens");

    private final String className;
    private final Map<String, Method> methods = new HashMap<>();
    private Object instance;
    private boolean broken;

    public KotlinObjectAccessor(@NotNull String className) {
        this.className = className;
    }

    /** The object instance, or null if it cannot be accessed. */
    public synchronized @Nullable Object instance() {
        if (this.broken) {
            return null;
        }
        if (this.instance == null) {
            try {
                this.instance = Class.forName(this.className).getField("INSTANCE").get(null);
            } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
                this.fail("INSTANCE", e);
            }
        }
        return this.instance;
    }

    /** Calls a public no-arg method on the object, or returns null if that is not possible. */
    public synchronized @Nullable Object call(@NotNull String methodName) {
        final Object target = this.instance();
        if (target == null) {
            return null;
        }
        try {
            Method method = this.methods.get(methodName);
            if (method == null) {
                method = target.getClass().getMethod(methodName);
                this.methods.put(methodName, method);
            }
            return method.invoke(target);
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            this.fail(methodName, e);
            return null;
        }
    }

    private void fail(String member, Throwable e) {
        this.broken = true;
        LOGGER.warn("Could not access {}.{}, falling back to SkySens' own implementation", this.className, member, e);
    }

}
