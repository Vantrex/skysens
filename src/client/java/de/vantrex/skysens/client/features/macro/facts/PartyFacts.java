package de.vantrex.skysens.client.features.macro.facts;

import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.skyblock.party.PartyRole;
import de.vantrex.skysens.client.skyblock.party.PartyTracker;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** The party as the macros see it. Every answer is UNKNOWN (or null) until the party state is known. */
public final class PartyFacts {

    private final PartyTracker tracker = PartyTracker.getInstance();

    public Tri inParty() {
        return this.tracker.getInParty().map(Tri::of).orElse(Tri.UNKNOWN);
    }

    /** FALSE when not in a party; UNKNOWN while the own rank isn't known. */
    public Tri isLeader() {
        final Tri party = this.inParty();
        if (party != Tri.TRUE) {
            return party;
        }
        final PartyRole role = this.tracker.getRole();
        if (role != null) {
            return Tri.of(role == PartyRole.LEADER);
        }
        return this.tracker.isLedByOtherMember() ? Tri.FALSE : Tri.UNKNOWN;
    }

    /** The party size including the player: 1 when known not to be in a party, null when unknown. */
    public @Nullable Integer size() {
        final Tri party = this.inParty();
        if (party == Tri.UNKNOWN) {
            return null;
        }
        if (party == Tri.FALSE) {
            return 1;
        }
        return this.membersMissing() ? null : this.tracker.getSize();
    }

    /** Whether another player is in the party; UNKNOWN while the member list is incomplete and doesn't have them. */
    public Tri hasMember(String name) {
        final Tri party = this.inParty();
        if (party != Tri.TRUE) {
            return party;
        }
        if (this.tracker.hasMember(name)) {
            return Tri.TRUE;
        }
        final boolean nameless = this.tracker.getMembers().stream().anyMatch(member -> member.getName() == null);
        return this.membersMissing() || nameless ? Tri.UNKNOWN : Tri.FALSE;
    }

    /** The leader's name, null if unknown or not in a party. */
    public @Nullable String leader() {
        return this.tracker.getLeader();
    }

    public List<String> memberNames() {
        return this.tracker.getMemberNames();
    }

    /** In a party but no other member is known: the list hasn't been read yet. */
    private boolean membersMissing() {
        return this.tracker.getMembers().isEmpty();
    }

}
