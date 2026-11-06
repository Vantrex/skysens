package de.vantrex.skysens.client.skyblock.sidebar;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.skyblock.event.GardenVisitorsChangeEvent;
import de.vantrex.skysens.client.skyblock.event.JacobsContestChangeEvent;
import de.vantrex.skysens.client.skyblock.event.KuudraTierChangeEvent;
import de.vantrex.skysens.client.skyblock.event.LocationChangeEvent;
import de.vantrex.skysens.client.skyblock.event.MiningEventChangeEvent;
import de.vantrex.skysens.client.skyblock.event.SidebarEventChangeEvent;
import de.vantrex.skysens.client.skyblock.event.SkyblockJoinEvent;
import de.vantrex.skysens.client.skyblock.event.SkyblockQuitEvent;
import de.vantrex.skysens.client.util.ScoreboardUtil;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the sidebar once a second on SkyBlock and exposes timed events (Spooky Festival, Traveling Zoo, New Year), the
 * mining event, Jacob's contest, the Kuudra tier, Garden visitors, Crystal Hollows heat and the Bingo profile.
 * <p>
 * <b>Tri-state:</b> everything is unknown until the first non-empty sidebar read since joining SkyBlock or changing
 * island ({@link #isKnown()}); the getters then return their "none" value, so callers that must tell unknown from
 * false check {@link #isKnown()} (or use the {@code Optional} variants, empty while unknown). State is rebuilt from
 * the sidebar after every join/island change, never from one-time messages. The first read only records the state and
 * fires no change events; later real changes do. Always active. Client thread.
 */
public final class SidebarEventTracker implements AfterTickFeature {

    @Getter
    private static final SidebarEventTracker instance = new SidebarEventTracker();

    private static final int INTERVAL_TICKS = 20;

    // Patterns match sidebar lines without formatting codes; see docs/specs/skyblock-events-research.md.
    private static final Pattern MINING_EVENT = Pattern.compile("^Event: (.+)$");
    private static final Pattern MINING_ZONE = Pattern.compile("^Zone: (.+)$");
    private static final String JACOBS = "Jacob's Contest";
    private static final Pattern KUUDRA = Pattern.compile("Kuudra's Hollow \\(T(\\d+)\\)");
    private static final Pattern KUUDRA_TOKENS = Pattern.compile("^Tokens: ([\\d,]+)$");
    private static final Pattern VISITORS = Pattern.compile("✌\\s*\\(\\s*(\\d+)\\D*?/\\s*(\\d+)");
    private static final Pattern HEAT = Pattern.compile("^Heat: (\\d+|IMMUNE)♨?$");
    private static final String BINGO_MARKER = "Ⓑ";
    private static final List<String> CROPS = List.of("Wheat", "Carrot", "Potato", "Pumpkin", "Melon", "Mushroom",
            "Cocoa Beans", "Cactus", "Sugar Cane", "Nether Wart");

    private record Snapshot(
            Map<SidebarEventType, String> timed,
            @Nullable String miningEvent,
            @Nullable String miningZone,
            boolean jacobsActive,
            @Nullable String jacobsCrop,
            int kuudraTier,
            int kuudraTokens,
            int visitorsCurrent,
            int visitorsMax,
            int heat,
            boolean heatImmune,
            boolean bingo) {
    }

    private @Nullable Snapshot current;
    private int ticks;

    private SidebarEventTracker() {
        EventListenerRegistry.REGISTRY.registerListener(event -> this.current = null, SkyblockQuitEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(event -> this.current = null, SkyblockJoinEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(event -> this.current = null, LocationChangeEvent.class);
    }

    // ---------------------------------------------------------------- read API

    /** Whether the sidebar has been read since joining / the last island change; false means every value is unknown. */
    public boolean isKnown() {
        return this.current != null;
    }

    /** Whether the timed event is shown; empty while unknown. */
    public @NotNull Optional<Boolean> getEventActive(@NotNull SidebarEventType type) {
        final Snapshot s = this.current;
        return s == null ? Optional.empty() : Optional.of(s.timed().containsKey(type));
    }

    /** Whether the timed event is shown; false while unknown (check {@link #isKnown()} to tell). */
    public boolean isEventActive(@NotNull SidebarEventType type) {
        return this.getEventActive(type).orElse(false);
    }

    /** The raw countdown text of the timed event (e.g. "12:34"), if shown. */
    public @NotNull Optional<String> getEventRemaining(@NotNull SidebarEventType type) {
        final Snapshot s = this.current;
        return s == null ? Optional.empty() : Optional.ofNullable(s.timed().get(type));
    }

    /** The mining event name as shown (e.g. "RAFFLE"), null if none or unknown. */
    public @Nullable String getMiningEvent() {
        final Snapshot s = this.current;
        return s == null ? null : s.miningEvent();
    }

    /** The zone of the mining event, null if none or unknown. */
    public @Nullable String getMiningEventZone() {
        final Snapshot s = this.current;
        return s == null ? null : s.miningZone();
    }

    /** Whether Jacob's contest is shown; empty while unknown. */
    public @NotNull Optional<Boolean> getJacobsContestActive() {
        final Snapshot s = this.current;
        return s == null ? Optional.empty() : Optional.of(s.jacobsActive());
    }

    /** The crop of the running contest if it is visible in the sidebar. */
    public @NotNull Optional<String> getJacobsCrop() {
        final Snapshot s = this.current;
        return s == null ? Optional.empty() : Optional.ofNullable(s.jacobsCrop());
    }

    /** The Kuudra tier 1-5 while in Kuudra, 0 otherwise or while unknown. */
    public int getKuudraTier() {
        final Snapshot s = this.current;
        return s == null ? 0 : s.kuudraTier();
    }

    /** The Kuudra tokens shown while in Kuudra. */
    public @NotNull OptionalInt getKuudraTokens() {
        final Snapshot s = this.current;
        return s == null || s.kuudraTokens() < 0 ? OptionalInt.empty() : OptionalInt.of(s.kuudraTokens());
    }

    /** The Garden visitor count shown, empty if not shown or unknown. */
    public @NotNull OptionalInt getGardenVisitors() {
        final Snapshot s = this.current;
        return s == null || s.visitorsCurrent() < 0 ? OptionalInt.empty() : OptionalInt.of(s.visitorsCurrent());
    }

    public @NotNull OptionalInt getGardenVisitorsMax() {
        final Snapshot s = this.current;
        return s == null || s.visitorsMax() < 0 ? OptionalInt.empty() : OptionalInt.of(s.visitorsMax());
    }

    /** The Crystal Hollows heat, empty if not shown (or immune, see {@link #isHeatImmune()}). */
    public @NotNull OptionalInt getHeat() {
        final Snapshot s = this.current;
        return s == null || s.heat() < 0 ? OptionalInt.empty() : OptionalInt.of(s.heat());
    }

    public boolean isHeatImmune() {
        final Snapshot s = this.current;
        return s != null && s.heatImmune();
    }

    /** Whether the profile is a Bingo profile (from the sidebar title); empty while unknown. */
    public @NotNull Optional<Boolean> isBingoProfile() {
        final Snapshot s = this.current;
        return s == null ? Optional.empty() : Optional.of(s.bingo());
    }

    // ---------------------------------------------------------------- polling

    @Override
    public void afterTick() {
        if (++this.ticks % INTERVAL_TICKS != 0 || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final List<String> lines = ScoreboardUtil.getSidebarLines();
        if (lines.isEmpty()) {
            // sidebar not there yet (join, server switch): stay as we are
            return;
        }
        final Snapshot next = this.parse(ScoreboardUtil.getSidebarTitle(), lines);
        final Snapshot previous = this.current;
        this.current = next;
        if (previous != null) {
            this.fireChanges(previous, next);
        }
    }

    private Snapshot parse(@Nullable String title, List<String> lines) {
        final Map<SidebarEventType, String> timed = new EnumMap<>(SidebarEventType.class);
        String miningEvent = null;
        String miningZone = null;
        boolean jacobs = false;
        String crop = null;
        int tier = 0;
        int tokens = -1;
        int visCur = -1;
        int visMax = -1;
        int heat = -1;
        boolean immune = false;
        for (int i = 0; i < lines.size(); i++) {
            final String line = lines.get(i);
            for (final SidebarEventType type : SidebarEventType.values()) {
                final Matcher m = type.pattern().matcher(line);
                if (m.matches()) {
                    timed.put(type, m.group(1));
                }
            }
            Matcher m = MINING_EVENT.matcher(line);
            if (m.matches()) {
                miningEvent = m.group(1).trim();
            }
            m = MINING_ZONE.matcher(line);
            if (m.matches()) {
                miningZone = m.group(1).trim();
            }
            if (line.contains(JACOBS)) {
                jacobs = true;
                // the crop's position is not verified: look at the lines right below
                for (int j = i + 1; j < Math.min(lines.size(), i + 4) && crop == null; j++) {
                    for (final String c : CROPS) {
                        if (lines.get(j).contains(c)) {
                            crop = c;
                            break;
                        }
                    }
                }
            }
            m = KUUDRA.matcher(line);
            if (m.find()) {
                tier = Integer.parseInt(m.group(1));
            }
            m = KUUDRA_TOKENS.matcher(line);
            if (m.matches()) {
                tokens = parseInt(m.group(1).replace(",", ""));
            }
            m = VISITORS.matcher(line);
            if (m.find()) {
                visCur = Integer.parseInt(m.group(1));
                visMax = Integer.parseInt(m.group(2));
            }
            m = HEAT.matcher(line);
            if (m.matches()) {
                if (m.group(1).equals("IMMUNE")) {
                    immune = true;
                } else {
                    heat = parseInt(m.group(1));
                }
            }
        }
        if (miningEvent == null) {
            miningZone = null;
        }
        if (tier == 0) {
            tokens = -1;
        }
        return new Snapshot(timed, miningEvent, miningZone, jacobs, crop, tier, tokens, visCur, visMax, heat, immune,
                title != null && title.contains(BINGO_MARKER));
    }

    private static int parseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void fireChanges(Snapshot old, Snapshot now) {
        for (final SidebarEventType type : SidebarEventType.values()) {
            final boolean was = old.timed().containsKey(type);
            final boolean is = now.timed().containsKey(type);
            if (was != is) {
                EventListenerRegistry.REGISTRY.fireEvent(new SidebarEventChangeEvent(type, is));
            }
        }
        if (!java.util.Objects.equals(old.miningEvent(), now.miningEvent())) {
            EventListenerRegistry.REGISTRY.fireEvent(new MiningEventChangeEvent(old.miningEvent(), now.miningEvent()));
        }
        if (old.jacobsActive() != now.jacobsActive() || !java.util.Objects.equals(old.jacobsCrop(), now.jacobsCrop())) {
            EventListenerRegistry.REGISTRY.fireEvent(new JacobsContestChangeEvent(now.jacobsActive(), now.jacobsCrop()));
        }
        if (old.kuudraTier() != now.kuudraTier()) {
            EventListenerRegistry.REGISTRY.fireEvent(new KuudraTierChangeEvent(now.kuudraTier()));
        }
        if (old.visitorsCurrent() != now.visitorsCurrent() || old.visitorsMax() != now.visitorsMax()) {
            EventListenerRegistry.REGISTRY.fireEvent(new GardenVisitorsChangeEvent(now.visitorsCurrent(), now.visitorsMax()));
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }
}
