package de.vantrex.skysens.client.config.ui.render;

import java.util.Locale;

/**
 * Selectable GUI font families. Keep in sync with the {@code guiFonts} list in build.gradle.kts, which generates
 * the font definition JSONs ({@code skysens:<prefix>_<weight>_<level>}) for every non-vanilla family.
 */
public enum FontFamily {
    INTER("Inter", "inter", Group.SANS),
    GEIST("Geist", "geist", Group.SANS),
    IBM_PLEX_SANS("IBM Plex Sans", "ibm_plex_sans", Group.SANS),
    DM_SANS("DM Sans", "dm_sans", Group.SANS),
    MANROPE("Manrope", "manrope", Group.SANS),
    ROBOTO("Roboto", "roboto", Group.SANS),
    NUNITO("Nunito", "nunito", Group.ROUNDED),
    OUTFIT("Outfit", "outfit", Group.ROUNDED),
    POPPINS("Poppins", "poppins", Group.ROUNDED),
    GEIST_MONO("Geist Mono", "geist_mono", Group.MONO),
    JETBRAINS_MONO("JetBrains Mono", "jetbrains_mono", Group.MONO),
    MINECRAFT("Minecraft", null, Group.MINECRAFT);

    public enum Group {
        SANS("Sans"), ROUNDED("Rounded / Geometric"), MONO("Monospace"), MINECRAFT("Minecraft");

        private final String title;

        Group(String title) {
            this.title = title;
        }

        public String title() {
            return title;
        }
    }

    public static final FontFamily DEFAULT = INTER;

    private final String displayName;
    private final String prefix;
    private final Group group;

    FontFamily(String displayName, String prefix, Group group) {
        this.displayName = displayName;
        this.prefix = prefix;
        this.group = group;
    }

    /** Persisted id (the enum name, e.g. {@code IBM_PLEX_SANS}). */
    public String id() {
        return name();
    }

    public String displayName() {
        return displayName;
    }

    /** File/id prefix of the generated font definitions, null for the vanilla font. */
    public String prefix() {
        return prefix;
    }

    public boolean vanilla() {
        return prefix == null;
    }

    public boolean monospace() {
        return group == Group.MONO;
    }

    public Group group() {
        return group;
    }

    /** Resolves a persisted id; unknown or null falls back to {@link #DEFAULT}. */
    public static FontFamily fromId(String id) {
        if (id != null) {
            String s = id.trim().toUpperCase(Locale.ROOT);
            for (FontFamily f : values()) {
                if (f.name().equals(s)) {
                    return f;
                }
            }
        }
        return DEFAULT;
    }
}
