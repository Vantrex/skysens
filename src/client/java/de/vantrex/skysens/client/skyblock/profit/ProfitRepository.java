package de.vantrex.skysens.client.skyblock.profit;

import com.google.gson.annotations.Expose;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/** All-time profit, saved to config/skysens/repositories/ProfitRepository.json. */
@NoArgsConstructor
public class ProfitRepository {

    @Expose
    public ProfitStats overall = new ProfitStats();

    /** The local date (ISO) that {@link #dayStats} belongs to, null before the first profit. */
    @Expose
    public String day;

    /** Overall profit and uptime made on {@link #day} in earlier game sessions. */
    @Expose
    public ProfitStats dayStats = new ProfitStats();

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
