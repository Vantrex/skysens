package de.vantrex.skysens.client.skyblock.diana;

import org.jetbrains.annotations.Nullable;


/** The mobs a Griffin burrow can spawn during the Mythological Ritual. */
public enum MythologicalMob {

    GAIA_CONSTRUCT("Gaia Construct", false),
    MINOTAUR("Minotaur", false),
    MINOS_CHAMPION("Minos Champion", false),
    SIAMESE_LYNXES("Siamese Lynxes", false),
    MINOS_HUNTER("Minos Hunter", false),
    CRETAN_BULL("Cretan Bull", false),
    HARPY("Harpy", false),
    STRANDED_NYMPH("Stranded Nymph", false),
    SPHINX("Sphinx", true),
    MINOS_INQUISITOR("Minos Inquisitor", true),
    KING_MINOS("King Minos", true),
    MANTICORE("Manticore", true);

    private final String displayName;
    private final boolean rare;

    MythologicalMob(String displayName, boolean rare) {
        this.displayName = displayName;
        this.rare = rare;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public boolean isRare() {
        return this.rare;
    }

    /** The mob with this colourless name, null if it isn't one. */
    public static @Nullable MythologicalMob fromName(String name) {
        final String trimmed = name.trim();
        for (final MythologicalMob mob : values()) {
            if (mob.displayName.equalsIgnoreCase(trimmed)) {
                return mob;
            }
        }
        return null;
    }

}
