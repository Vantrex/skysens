package de.vantrex.skysens.client.dungeon.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DungeonFloorEnum {

    ENTRANCE("E", DungeonModeEnum.NORMAL),
    FLOOR_1("F1", DungeonModeEnum.NORMAL),
    FLOOR_2("F2", DungeonModeEnum.NORMAL),
    FLOOR_3("F3", DungeonModeEnum.NORMAL),
    FLOOR_4("F4", DungeonModeEnum.NORMAL),
    FLOOR_5("F5", DungeonModeEnum.NORMAL),
    FLOOR_6("F6", DungeonModeEnum.NORMAL),
    FLOOR_7("F7", DungeonModeEnum.NORMAL),
    MASTER_FLOOR_1("M1", DungeonModeEnum.MASTER),
    MASTER_FLOOR_2("M2", DungeonModeEnum.MASTER),
    MASTER_FLOOR_3("M3", DungeonModeEnum.MASTER),
    MASTER_FLOOR_4("M4", DungeonModeEnum.MASTER),
    MASTER_FLOOR_5("M5", DungeonModeEnum.MASTER),
    MASTER_FLOOR_6("M6", DungeonModeEnum.MASTER),
    MASTER_FLOOR_7("M7", DungeonModeEnum.MASTER);

    private final String key;
    private final DungeonModeEnum modeEnum;

}
