package de.vantrex.skysens.client.features.dungeon;

import com.google.gson.annotations.Expose;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;
import java.util.Map;

/** Dungeon run stats per floor, saved to config/skysens/repositories/DungeonRunRepository.json. */
@NoArgsConstructor
public class DungeonRunRepository {

    /** Floor ("M7", "F5") to its stats over all runs. Ordered, the floor with the most recent run is last. */
    @Expose
    public Map<String, DungeonRunStats.FloorStats> allTime = new LinkedHashMap<>();

    /** The local date (ISO) that {@link #dayFloors} belongs to, null before the first run. */
    @Expose
    public String day;

    /** Floor to its stats of the runs on {@link #day}, across game sessions. */
    @Expose
    public Map<String, DungeonRunStats.FloorStats> dayFloors = new LinkedHashMap<>();

}
