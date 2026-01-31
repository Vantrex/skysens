package de.vantrex.skysens.client.handler;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.feature.FeatureRegistry;
import de.vantrex.skysens.client.service.FeatureService;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class TickHandlers {

    private static final FeatureRegistry FEATURE_REGISTRY = FeatureService.getInstance().getFeatureRegistry();

    public static void register() {
        ClientTickEvents.END_CLIENT_TICK
                .register(minecraftClient -> {
                    if (!SkysensClient.getInstance().isOnSkyBlock()) {
                        return;
                    }
                    for (final var afterTickFeature : FEATURE_REGISTRY.getAfterTickFeature()) {
                        if (afterTickFeature.isActive()) {
                            afterTickFeature.afterTick();
                        }
                    }
                });
    }

}
