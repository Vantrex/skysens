package de.vantrex.skysens.client.skyblock.party;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Another member of the player's party. Members from chat messages have a name but maybe no UUID, members from the
 * Hypixel Mod API have a UUID but only a name if they are in the same lobby (or were named in chat before).
 */
@Data
@AllArgsConstructor
public final class PartyMember {

    private @Nullable UUID uuid;
    private @Nullable String name;

    boolean hasName(@Nullable String other) {
        return this.name != null && this.name.equalsIgnoreCase(other);
    }

}
