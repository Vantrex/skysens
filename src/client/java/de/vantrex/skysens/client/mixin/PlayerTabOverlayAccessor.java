package de.vantrex.skysens.client.mixin;

import net.minecraft.client.gui.components.PlayerTabOverlay;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(PlayerTabOverlay.class)
public interface PlayerTabOverlayAccessor {

    /** The tab list entries in the order they are displayed. */
    @Invoker("getPlayerInfos")
    List<PlayerInfo> skysens$getPlayerInfos();

}
