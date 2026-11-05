package de.vantrex.skysens.client.features.volcano;

import de.vantrex.skysens.client.util.TabListUtil;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

/** Reads the "Volcano: CHILL" line of the Crimson Isle tab list. */
@UtilityClass
class VolcanoTabList {

    private static final String PREFIX = "Volcano:";

    /** The text after "Volcano:", null if the tab list has no volcano line. */
    @Nullable String read() {
        for (final String line : TabListUtil.lines()) {
            if (line.startsWith(PREFIX)) {
                return line.substring(PREFIX.length()).trim();
            }
        }
        return null;
    }

}
