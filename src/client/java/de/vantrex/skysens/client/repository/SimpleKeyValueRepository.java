package de.vantrex.skysens.client.repository;

import com.google.gson.annotations.Expose;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Synchronized;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class SimpleKeyValueRepository {

    @Expose
    @Setter
    private Map<String, Object> dataStore = new HashMap<>();

    @Synchronized
    public void put(final @NotNull String key, final @Nullable Object value) {
        if (value == null) {
            this.remove(key);
            return;
        };
        dataStore.put(key, value);
    }

    @Synchronized
    public <T> T get(final @NotNull String key) {
        return (T) dataStore.get(key);
    }

    @Synchronized
    public void remove(final @NotNull String key) {
        dataStore.remove(key);
    }

    @Synchronized
    public Map<String, Object> getDataStore() {
        return dataStore;
    }
}
