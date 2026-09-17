package de.vantrex.skysens;

import net.fabricmc.api.ModInitializer;
import net.minecraft.world.level.block.Blocks;

public class Skysens implements ModInitializer {

    @Override
    public void onInitialize() {
        // Verify Minecraft classes are on compile classpath
        var block = Blocks.STONE;
    }
}
