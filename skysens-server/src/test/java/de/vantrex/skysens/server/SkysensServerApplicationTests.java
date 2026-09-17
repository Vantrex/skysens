package de.vantrex.skysens.server;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Smoke test: the Spring context loads. That is the entire scope (§11) — it proves
 * the bean graph is wired and, implicitly, that neither Minecraft nor Fabric is
 * anywhere on this module's classpath.
 */
@SpringBootTest
@ActiveProfiles("dev")
class SkysensServerApplicationTests {

    @Test
    void contextLoads() {
    }
}
