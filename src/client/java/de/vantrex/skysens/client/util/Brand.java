package de.vantrex.skysens.client.util;

import lombok.experimental.UtilityClass;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import org.jetbrains.annotations.NotNull;

/** The SkySens brand gradient (§5 dark purple over §d light purple into pink), for chat prefixes, titles and the config GUI. */
@UtilityClass
public class Brand {

    /** Gradient stops, left to right: §5, §d, pink. */
    private final int[] GRADIENT = {0xAA00AA, 0xFF55FF, 0xFF77BB};

    public final String NAME = "SkySens";

    /** "[SkySens] " in the brand gradient, to put in front of chat messages. */
    public @NotNull MutableComponent prefix() {
        return gradient("[" + NAME + "]", Style.EMPTY).append(Component.literal(" "));
    }

    /** "SkySens" in the brand gradient. */
    public @NotNull MutableComponent name() {
        return gradient(NAME, Style.EMPTY);
    }

    /** {@code text} with every character coloured along the brand gradient, on top of {@code base}. */
    public @NotNull MutableComponent gradient(final String text, final Style base) {
        final MutableComponent result = Component.empty();
        final int length = text.codePointCount(0, text.length());
        int index = 0;
        for (int i = 0; i < text.length(); ) {
            final int codePoint = text.codePointAt(i);
            final float t = length <= 1 ? 0f : (float) index / (length - 1);
            result.append(Component.literal(Character.toString(codePoint)).setStyle(base.withColor(colourAt(t))));
            i += Character.charCount(codePoint);
            index++;
        }
        return result;
    }

    /** RGB of the gradient at {@code t} (0 = left end, 1 = right end). */
    public int colourAt(final float t) {
        final float scaled = Math.clamp(t, 0f, 1f) * (GRADIENT.length - 1);
        final int segment = Math.min((int) scaled, GRADIENT.length - 2);
        final float f = scaled - segment;
        final int from = GRADIENT[segment];
        final int to = GRADIENT[segment + 1];
        return lerp(from >> 16 & 0xFF, to >> 16 & 0xFF, f) << 16
                | lerp(from >> 8 & 0xFF, to >> 8 & 0xFF, f) << 8
                | lerp(from & 0xFF, to & 0xFF, f);
    }

    private int lerp(final int from, final int to, final float f) {
        return Math.round(from + (to - from) * f);
    }
}
