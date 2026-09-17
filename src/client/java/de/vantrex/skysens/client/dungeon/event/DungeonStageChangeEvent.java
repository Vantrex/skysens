package de.vantrex.skysens.client.dungeon.event;

import de.vantrex.skysens.client.dungeon.enums.DungeonStageEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class DungeonStageChangeEvent extends DungeonEvent {

    private final DungeonStageEnum oldStage;
    private final DungeonStageEnum newStage;

}
