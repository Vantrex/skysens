package de.vantrex.skysens.client.platform.hook;

import com.mojang.blaze3d.platform.InputConstants;
import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.features.warp.WarpToMenuScreen;
import de.vantrex.skysens.client.config.SkysensConfig;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/** Key mappings, set in Controls under "SkySens". All unbound by default. */
public class KeyHandlers {

    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("skysens", "main"));

    public static void register() {
        final KeyMapping warpToMenu = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.skysens.warpto_menu",
                InputConstants.Type.KEYSYM, InputConstants.UNKNOWN.getValue(), CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (warpToMenu.consumeClick()) {
                if (SkysensClient.getInstance().isOnSkyBlock() && SkysensConfig.getInstance().chatCommandsCategory.warpToMenu) {
                    WarpToMenuScreen.open();
                }
            }
        });
    }

}
