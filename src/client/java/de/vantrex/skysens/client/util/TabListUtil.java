package de.vantrex.skysens.client.util;

import de.vantrex.skysens.client.platform.mixin.PlayerTabOverlayAccessor;
import lombok.experimental.UtilityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class TabListUtil {

    /** The tab list entries without formatting codes and surrounding spaces, empty if not connected. */
    public @NotNull List<String> lines() {
        final Minecraft client = Minecraft.getInstance();
        final List<String> lines = new ArrayList<>();
        if (client.getConnection() == null) {
            return lines;
        }
        for (final PlayerInfo info : ((PlayerTabOverlayAccessor) client.gui.hud.getTabList()).skysens$getPlayerInfos()) {
            final Component displayName = info.getTabListDisplayName();
            final String line = displayName == null ? null : ChatFormatting.stripFormatting(displayName.getString());
            lines.add(line == null ? "" : line.trim());
        }
        return lines;
    }

}
