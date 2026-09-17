package de.vantrex.skysens.client.dungeon.event;

import de.vantrex.skysens.common.domain.dungeon.DungeonStageEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DungeonStageChangeEvent extends DungeonEvent {

    private final DungeonStageEnum oldStage;
    private final DungeonStageEnum newStage;

}
