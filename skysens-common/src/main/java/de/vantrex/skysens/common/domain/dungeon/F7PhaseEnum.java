package de.vantrex.skysens.common.domain.dungeon;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum F7PhaseEnum {
    CLEARING,
    PRE_BOSS,
    MAXOR,
    STORM,
    GOLDOR,
    NECRON,
    DRAGONS,
    UNKNOWN
}
