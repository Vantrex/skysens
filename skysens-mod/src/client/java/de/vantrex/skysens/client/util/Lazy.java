package de.vantrex.skysens.client.util;

import com.google.common.base.Suppliers;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public class Lazy<T> {

    private final Supplier<T> supplier;

    public Lazy(final @NotNull Supplier<T> delegate) {
        this.supplier = Suppliers.memoize(delegate::get);
    }

    public T get() {
        return this.supplier.get();
    }

}
