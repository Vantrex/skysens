package de.vantrex.skysens.client.features.macro.facts;

import de.vantrex.skysens.client.features.macro.condition.Tri;
import de.vantrex.skysens.client.skyblock.mayor.MayorService;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** The mayor and the minister. UNKNOWN until the election data was fetched successfully once. */
public final class MayorFacts {

    private final MayorService service = MayorService.getInstance();

    public MayorFacts() {
        // time guarded, so this is cheap; makes sure the data gets fetched even if no other feature asked for it
        this.service.refreshIfNeeded();
    }

    public boolean known() {
        return this.service.isDataKnown();
    }

    public @Nullable String mayor() {
        return this.known() ? this.service.getMayorName() : null;
    }

    public @Nullable String minister() {
        return this.known() ? this.service.getMinisterName() : null;
    }

    /** Whether the mayor (and with {@code includeMinister} the minister) has the perk; UNKNOWN while not fetched. */
    public Tri hasPerk(String perk, boolean includeMinister) {
        if (!this.known()) {
            return Tri.UNKNOWN;
        }
        final MayorService.Election election = this.service.getElection();
        for (String name : election.mayorPerks()) {
            if (name.equalsIgnoreCase(perk)) {
                return Tri.TRUE;
            }
        }
        if (includeMinister) {
            for (String name : election.ministerPerks()) {
                if (name.equalsIgnoreCase(perk)) {
                    return Tri.TRUE;
                }
            }
        }
        return Tri.FALSE;
    }

    /** Names for the editor drop-downs: the live mayor and minister. Also starts a fetch if none ran yet. */
    public static List<String> liveNames() {
        MayorService.getInstance().refreshIfNeeded();
        final MayorService.Election election = MayorService.getInstance().getElection();
        final List<String> out = new ArrayList<>();
        if (election.mayor() != null) {
            out.add(election.mayor());
        }
        if (election.minister() != null) {
            out.add(election.minister());
        }
        return out;
    }

    public static List<String> livePerks() {
        final MayorService.Election election = MayorService.getInstance().getElection();
        final List<String> out = new ArrayList<>(election.mayorPerks());
        out.addAll(election.ministerPerks());
        return out;
    }

}
