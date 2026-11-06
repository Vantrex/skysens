package de.vantrex.skysens.client.skyblock.pet;

/** What made {@code PetTracker} notice that the active pet changed. */
public enum PetChangeSource {
    /** "You summoned your X!" / "You despawned your X!" without a pets menu click before it. */
    CHAT,
    /** "Autopet equipped your [Lvl N] X! VIEW RULE". */
    AUTOPET,
    /** A click in the /pets menu, or the menu showing a different active pet. */
    PETS_MENU,
    /** A loadout switch (see {@code PetTracker}: detected by a loadout chat message shortly before a change). */
    LOADOUT,
    /** The "Pet:" tab list widget, the fallback that catches everything the other sources miss. */
    TAB_LIST
}
