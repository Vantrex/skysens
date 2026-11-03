package de.vantrex.skysens.client.skyblock.party;

/** Why the player or a party member is no longer in the party. */
public enum PartyLeaveReason {

    /** Left on their own ("You left the party.", "ReZuni has left the party."). */
    LEFT,
    /** Kicked by the leader or a moderator ("You have been kicked from the party by ...", "... has been removed from the party."). */
    KICKED,
    /** Removed for being offline ("... was removed from your party because they disconnected.", "Kicked ... because they were offline."). */
    DISCONNECTED,
    /** The whole party was disbanded, by the leader or because it was empty. */
    DISBANDED,
    /** Hypixel says we are not in a party although we thought so, without saying why. */
    UNKNOWN

}
