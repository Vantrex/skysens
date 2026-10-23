package de.vantrex.skysens.client.features.warp;

import com.google.gson.annotations.Expose;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Co-op members last read from the tab list, saved to config/skysens/repositories/CoopRepository.json. */
@NoArgsConstructor
public class CoopRepository {

    /** The SkyBlock profile we were on last, e.g. "Lemon". */
    @Expose
    public String lastProfile;

    /** Profile name to its co-op. */
    @Expose
    public Map<String, Coop> profiles = new HashMap<>();

    @NoArgsConstructor
    public static class Coop {
        /** The other co-op members, without us. */
        @Expose
        public List<Member> members = new ArrayList<>();
        /** When the members were read from the tab list. */
        @Expose
        public long lastSeenAt;
    }

    @NoArgsConstructor
    public static class Member {
        @Expose
        public String name;
        /** Whether the member was online when last read. */
        @Expose
        public boolean online;
        /** How long the member has been offline as Hypixel shows it (e.g. "12d"), null if online. */
        @Expose
        public String offlineFor;

        public Member(String name, boolean online, String offlineFor) {
            this.name = name;
            this.online = online;
            this.offlineFor = offlineFor;
        }
    }

}
