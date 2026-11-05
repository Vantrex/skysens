package de.vantrex.skysens.client.skyblock.event;

import de.vantrex.skysens.client.core.event.SkysensEvent;
import de.vantrex.skysens.client.skyblock.dungeon.DungeonPartyMember;
import lombok.Data;

import java.util.List;

/** Fired when the classes, levels or alive state in the dungeon party changed, or the party was cleared (empty list). */
@Data
public final class DungeonPartyClassesUpdateEvent implements SkysensEvent {

    private final List<DungeonPartyMember> members;

}
