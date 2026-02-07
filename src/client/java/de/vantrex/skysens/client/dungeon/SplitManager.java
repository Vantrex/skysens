package de.vantrex.skysens.client.dungeon;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.vantrex.skysens.client.dungeon.model.DungeonSplit;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.Getter;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SplitManager {

    private static final SplitManager INSTANCE = new SplitManager();
    private static final Gson GSON = new Gson();

    @Getter
    private Map<String, List<DungeonSplit>> splits = Collections.emptyMap();

    public static SplitManager getInstance() {
        return INSTANCE;
    }

    public void loadSplits() {
        try {
            // Loading from classpath resource
            InputStream stream = getClass().getClassLoader().getResourceAsStream("splits.json");
            if (stream == null) {
                ClientUtil.sendDebug("Failed to find splits.json in resources");
                return;
            }
            try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Type type = new TypeToken<Map<String, List<DungeonSplit>>>() {}.getType();
                this.splits = GSON.fromJson(reader, type);
                ClientUtil.sendDebug("Loaded " + splits.size() + " floor configurations from splits.json");
            }
        } catch (Exception e) {
            ClientUtil.sendDebug("Failed to load splits.json: " + e.getMessage());
        }
    }

    public List<DungeonSplit> getSplitsForFloor(String floor) {
        return splits.getOrDefault(floor, Collections.emptyList());
    }
}
