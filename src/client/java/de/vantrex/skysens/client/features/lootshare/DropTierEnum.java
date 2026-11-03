package de.vantrex.skysens.client.features.lootshare;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

/** Hypixel's rare drop announcements, from least to most rare. */
@Getter
@RequiredArgsConstructor
public enum DropTierEnum {

    RARE("RARE DROP!", "Rare"),
    VERY_RARE("VERY RARE DROP!", "Very Rare"),
    CRAZY_RARE("CRAZY RARE DROP!", "Crazy Rare"),
    INSANE("INSANE DROP!", "Insane"),
    PRAY_TO_RNGESUS("PRAY TO RNGESUS DROP!", "Pray to RNGesus");

    /** How the drop message starts, e.g. "RARE DROP!". */
    private final String messagePrefix;
    private final String displayName;

    public static @Nullable DropTierEnum fromPrefix(String prefix) {
        for (final DropTierEnum tier : values()) {
            if (tier.messagePrefix.equals(prefix)) {
                return tier;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return this.displayName;
    }

}
