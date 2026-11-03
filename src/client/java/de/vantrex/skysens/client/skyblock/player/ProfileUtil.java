package de.vantrex.skysens.client.skyblock.player;

import de.vantrex.skysens.client.platform.mixin.PlayerTabOverlayAccessor;
import lombok.experimental.UtilityClass;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The SkyBlock profile name ("Lemon") and the tab list lines it is read from. Client thread. */
@UtilityClass
public class ProfileUtil {

    private static final Pattern PROFILE_PATTERN = Pattern.compile("^Profile: (\\w+)");

    /** The tab list entries without formatting codes and surrounding spaces, empty if not connected. */
    public @NotNull List<String> tabListLines() {
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

    /** The profile name from the tab list right now, null if the tab list doesn't show it. */
    public @Nullable String currentProfile() {
        return parseProfile(tabListLines());
    }

    /** The profile name from the "Profile: Lemon" tab list line, null if there is none. */
    public @Nullable String parseProfile(@NotNull List<String> lines) {
        for (final String line : lines) {
            final Matcher matcher = PROFILE_PATTERN.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

}
