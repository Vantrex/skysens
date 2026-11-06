package de.vantrex.skysens.client.features.macro.condition;

/** Tri-state condition result. Runtime only, never saved. UNKNOWN never matches, not even negated. */
public enum Tri {
    TRUE, FALSE, UNKNOWN;

    public static Tri of(boolean b) {
        return b ? TRUE : FALSE;
    }

    public Tri not() {
        return this == UNKNOWN ? UNKNOWN : (this == TRUE ? FALSE : TRUE);
    }

    public boolean matches() {
        return this == TRUE;
    }
}
