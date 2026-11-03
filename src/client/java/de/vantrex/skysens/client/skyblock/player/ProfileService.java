package de.vantrex.skysens.client.skyblock.player;

import de.vantrex.skysens.client.core.event.EventListenerRegistry;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.skyblock.event.ProfileChangeEvent;
import de.vantrex.skysens.client.skyblock.event.SkyblockJoinEvent;
import de.vantrex.skysens.client.skyblock.event.SkyblockQuitEvent;
import de.vantrex.skysens.client.skyblock.location.LocationService;
import de.vantrex.skysens.client.skyblock.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.util.ChatUtil;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.TabListUtil;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Knows the current SkyBlock profile name ("Lemon") and, once Hypixel sent it, its id, and fires a
 * {@link ProfileChangeEvent} when the name changes.
 * <p>
 * Hypixel names the profile in chat when joining SkyBlock ("You are playing on profile: Lemon (Co-op)") and when
 * switching ("Your profile was changed to: Lemon"). Right after the join message it sends the id ("Profile ID:
 * 691d6a3b-..."). Unlike the name it isn't reused when a profile is deleted and a new one gets its name, so prefer
 * it for storing data per profile. In case the join message was missed, the "Profile: Lemon" line of the tab list is
 * read once after joining; it may take a few ticks to show up, so it is retried for a short while. The tab list has
 * no id. Client thread.
 */
public final class ProfileService implements GameMessageListeningFeature, AfterTickFeature {

    @Getter
    private static final ProfileService instance = new ProfileService();

    private static final Pattern CHAT_PATTERN = Pattern.compile(
            "^(?:You are playing on profile|Your profile was changed to): (\\w+)");
    private static final Pattern ID_PATTERN = Pattern.compile("^Profile ID: ([0-9a-fA-F-]{36})$");
    private static final Pattern TAB_PATTERN = Pattern.compile("^Profile: (\\w+)");
    /** How long after joining SkyBlock the tab list is looked at for the profile name. */
    private static final int TAB_CHECK_TICKS = 100;

    /** The current profile name, null while unknown. */
    @Getter
    private @Nullable String currentProfile;
    /** The id of the current profile, null until Hypixel sent it for this profile. */
    @Getter
    private @Nullable String currentProfileId;
    /** Ticks left to find the profile in the tab list after joining, 0 if not looking. */
    private int tabCheckTicks;

    private ProfileService() {
        EventListenerRegistry.REGISTRY.registerListener(event -> this.tabCheckTicks = TAB_CHECK_TICKS, SkyblockJoinEvent.class);
        EventListenerRegistry.REGISTRY.registerListener(event -> {
            this.tabCheckTicks = 0;
            this.setProfile(null);
        }, SkyblockQuitEvent.class);
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay) {
            return;
        }
        final String text = ChatUtil.strip(message.getString());
        final Matcher matcher = CHAT_PATTERN.matcher(text);
        // a co-op profile is followed by " (Co-op)"
        if (matcher.find()) {
            this.tabCheckTicks = 0;
            this.setProfile(matcher.group(1));
            return;
        }
        final Matcher id = ID_PATTERN.matcher(text);
        if (id.matches() && !id.group(1).equals(this.currentProfileId)) {
            this.currentProfileId = id.group(1);
            ClientUtil.sendDebug("Profile ID: " + this.currentProfileId);
        }
    }

    @Override
    public void afterTick() {
        if (this.tabCheckTicks <= 0) {
            return;
        }
        this.tabCheckTicks--;
        if (this.currentProfile != null) {
            this.tabCheckTicks = 0;
            return;
        }
        // the Rift shows the profile name reversed in the tab list, rely on the chat message there
        if (LocationService.getInstance().getCurrentLocation() == SkyblockLocationEnum.RIFT_DIMENSION) {
            return;
        }
        final String profile = parseProfile(TabListUtil.lines());
        if (profile != null) {
            this.tabCheckTicks = 0;
            this.setProfile(profile);
        }
    }

    private void setProfile(@Nullable String profile) {
        if (Objects.equals(this.currentProfile, profile)) {
            return;
        }
        final String old = this.currentProfile;
        this.currentProfile = profile;
        // the id belonged to the old profile, the new one's follows in chat
        this.currentProfileId = null;
        ClientUtil.sendDebug("Profile: " + old + " -> " + profile);
        EventListenerRegistry.REGISTRY.fireEvent(new ProfileChangeEvent(old, profile));
    }

    /** The profile name from the "Profile: Lemon" tab list line, null if there is none. */
    static @Nullable String parseProfile(@NotNull List<String> lines) {
        for (final String line : lines) {
            final Matcher matcher = TAB_PATTERN.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    @Override
    public boolean isActive() {
        return true;
    }

}
