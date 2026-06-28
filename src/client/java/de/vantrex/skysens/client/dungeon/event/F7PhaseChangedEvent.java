package de.vantrex.skysens.client.dungeon.event;

import de.vantrex.skysens.client.dungeon.enums.F7PhaseEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class F7PhaseChangedEvent extends DungeonEvent {

    private final F7PhaseEnum oldPhase;
    private final F7PhaseEnum newPhase;

}
