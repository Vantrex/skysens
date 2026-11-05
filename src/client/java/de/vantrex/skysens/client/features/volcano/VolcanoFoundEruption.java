package de.vantrex.skysens.client.features.volcano;

import de.vantrex.skysens.client.SkysensClient;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/**
 * The eruption we got a title for on our current Crimson Isle server, for {@code !warpme}. Set by
 * {@link VolcanoEruptionFeature}, cleared when we leave the server or the eruption ends. Client thread only.
 */
@UtilityClass
class VolcanoFoundEruption {

    private @Nullable String server;
    private @Nullable VolcanoExplosivity explosivity;

    void set(String foundServer, VolcanoExplosivity foundExplosivity) {
        server = foundServer;
        explosivity = foundExplosivity;
    }

    void clear() {
        server = null;
        explosivity = null;
    }

    /** The eruption, if one was found and we are still on its server. */
    Optional<VolcanoExplosivity> current() {
        if (server == null || !server.equals(SkysensClient.getInstance().getServerName())) {
            return Optional.empty();
        }
        return Optional.ofNullable(explosivity);
    }

}
