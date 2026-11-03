package de.vantrex.skysens.client.skyblock.coop;

import com.google.gson.annotations.Expose;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Co-op members last read from the tab list, saved to config/skysens/repositories/CoopRepository.json. Don't rename
 * the class or its fields, they are the file name and the JSON keys.
 */
@NoArgsConstructor
public class CoopRepository {

    /** The SkyBlock profile we were on last, e.g. "Lemon". */
    @Expose
    public String lastProfile;

    /** The id of {@link #lastProfile}, null if Hypixel hadn't sent it. */
    @Expose
    public String lastProfileId;

    /** SkyBlock profile id to its co-op; keyed by the profile name ("Lemon") while the id isn't known. */
    @Expose
    public Map<String, Coop> profiles = new HashMap<>();

    /**
     * The co-op of the profile by its id, or by its name while the id is unknown, null if there is none. Once the id
     * is known, a co-op stored under the name is moved to the id.
     */
    public @Nullable Coop find(@Nullable String id, @NotNull String name) {
        if (id == null) {
            return this.named(this.profiles.get(name), name);
        }
        Coop coop = this.profiles.get(id);
        if (coop == null) {
            coop = this.profiles.remove(name);
            if (coop != null) {
                this.profiles.put(id, coop);
            }
        }
        return this.named(coop, name);
    }

    /** Like {@link #find}, but adds an empty co-op if there is none. */
    public @NotNull Coop coop(@Nullable String id, @NotNull String name) {
        final Coop coop = this.find(id, name);
        if (coop != null) {
            return coop;
        }
        final Coop created = new Coop();
        created.name = name;
        this.profiles.put(id != null ? id : name, created);
        return created;
    }

    private @Nullable Coop named(@Nullable Coop coop, String name) {
        if (coop != null) {
            coop.name = name;
        }
        return coop;
    }

    @NoArgsConstructor
    public static class Coop {
        /** The profile name ("Lemon") it was last seen with, to tell profiles apart in the file. */
        @Expose
        public String name;
        /** The other co-op members, without us. */
        @Expose
        public List<Member> members = new ArrayList<>();
        /** When the members were read from the tab list, 0 if they are only known from chat. */
        @Expose
        public long lastSeenAt;
    }

    @NoArgsConstructor
    @EqualsAndHashCode
    public static class Member {
        @Expose
        public String name;
        /** Whether the member was online when last read from the tab list or seen joining/leaving in chat. */
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
