package de.vantrex.skysens.common;

import de.vantrex.skysens.common.version.ApiVersion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Smoke test for skysens-common. The real assertion is structural: this module
 * compiles and runs with neither Minecraft, Fabric nor Spring on its classpath.
 */
class CommonModuleTest {

    @Test
    void currentVersionIsCompatibleWithItself() {
        assertTrue(ApiVersion.isCompatible(ApiVersion.CURRENT));
    }

    @Test
    void futureVersionIsNotCompatible() {
        assertFalse(ApiVersion.isCompatible(ApiVersion.CURRENT + 1));
    }
}
