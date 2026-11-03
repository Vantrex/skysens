package de.vantrex.skysens.client.skyblock.party;

import net.hypixel.modapi.packet.impl.clientbound.ClientboundPartyInfoPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A party member's rank, highest first. */
public enum PartyRole {

    /** Owns the party: can do everything, e.g. /party warp, /party disband, /party transfer. */
    LEADER,
    /** "Party Moderator": can e.g. invite and kick members. */
    MODERATOR,
    MEMBER;

    /** Whether this rank is the given one or higher. */
    public boolean isAtLeast(@NotNull PartyRole other) {
        return this.ordinal() <= other.ordinal();
    }

    static @Nullable PartyRole of(@Nullable ClientboundPartyInfoPacket.PartyRole role) {
        if (role == null) {
            return null;
        }
        return switch (role) {
            case LEADER -> LEADER;
            case MOD -> MODERATOR;
            case MEMBER -> MEMBER;
        };
    }

    /** The rank in "Party Leader", "Party Moderator(s)", "Party Member(s)". */
    static @Nullable PartyRole ofTitle(@NotNull String title) {
        return switch (title) {
            case "Leader" -> LEADER;
            case "Moderator", "Moderators" -> MODERATOR;
            case "Member", "Members" -> MEMBER;
            default -> null;
        };
    }

}
