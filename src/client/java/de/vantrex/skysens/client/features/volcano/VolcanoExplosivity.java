package de.vantrex.skysens.client.features.volcano;

import lombok.Getter;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

/**
 * The state of the Blazing Volcano on the Crimson Isle, as the tab list shows it ("Volcano: CHILL"). An eruption has
 * one of the explosivity levels, {@link #INACTIVE} means it isn't erupting. Chances from the wiki.
 */
@Getter
public enum VolcanoExplosivity {

    INACTIVE(ChatFormatting.GRAY),
    /** 27.12% */
    CHILL(ChatFormatting.AQUA),
    /** 23.75% */
    LOW(ChatFormatting.GREEN),
    /** 20.34% */
    DISRUPTIVE(ChatFormatting.YELLOW),
    /** 16.95% */
    MEDIUM(ChatFormatting.GOLD),
    /** 6.78% */
    HIGH(ChatFormatting.RED),
    /** 3.39% */
    EXPLOSIVE(ChatFormatting.DARK_RED),
    /** 1.69% */
    CATACLYSMIC(ChatFormatting.DARK_PURPLE);

    private final ChatFormatting color;

    VolcanoExplosivity(ChatFormatting color) {
        this.color = color;
    }

    public boolean isErupting() {
        return this != INACTIVE;
    }

    /** The level for the text after "Volcano:" in the tab list, null if unknown. */
    public static @Nullable VolcanoExplosivity fromTabList(String text) {
        final String name = text.trim().toUpperCase(Locale.ROOT);
        for (final VolcanoExplosivity explosivity : values()) {
            if (explosivity.name().equals(name)) {
                return explosivity;
            }
        }
        return null;
    }

}
