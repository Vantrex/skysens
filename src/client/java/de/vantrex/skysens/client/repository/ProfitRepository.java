package de.vantrex.skysens.client.repository;

import com.google.gson.annotations.Expose;
import de.vantrex.skysens.client.profit.ProfitStats;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/** All-time profit, saved to config/skysens/repositories/ProfitRepository.json. */
@NoArgsConstructor
public class ProfitRepository {

    @Expose
    public ProfitStats overall = new ProfitStats();

    /** Activity name ("Dungeons", "Slayer", "Crimson Isle", ...) to its profit. */
    @Expose
    public Map<String, ProfitStats> activities = new HashMap<>();

    @Expose
    public DungeonChestStats dungeonChests = new DungeonChestStats();

    @NoArgsConstructor
    public static class DungeonChestStats {
        @Expose
        public long chests;
        @Expose
        public double revenue;
        @Expose
        public double cost;

        public double profit() {
            return this.revenue - this.cost;
        }

        public void add(double revenue, double cost) {
            this.chests++;
            this.revenue += revenue;
            this.cost += cost;
        }

        public void reset() {
            this.chests = 0;
            this.revenue = 0;
            this.cost = 0;
        }
    }

}
