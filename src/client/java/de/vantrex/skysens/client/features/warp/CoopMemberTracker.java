package de.vantrex.skysens.client.features.warp;

import de.vantrex.skysens.client.platform.mixin.PlayerTabOverlayAccessor;
import de.vantrex.skysens.client.features.warp.CoopRepository.Coop;
import de.vantrex.skysens.client.features.warp.CoopRepository.Member;
import de.vantrex.skysens.client.persistence.RepositoryRegistry;
import lombok.Getter;
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

/**
 * Reads the co-op members from the "Coop (n)" tab list widget, which Hypixel only shows on the private island:
 * <pre>
 * Coop (3)
 * [470] Vantrex ♲
 * ReZuni
 * FaiN69 (Offline 12d)
 * </pre>
 * The members are cached per profile in the {@link CoopRepository}, so the warp menu also works away from the
 * island and after a restart. All methods run on the client thread.
 */
public final class CoopMemberTracker {

    private static final Pattern PROFILE_PATTERN = Pattern.compile("^Profile: (\\w+)");
    private static final Pattern HEADER_PATTERN = Pattern.compile("^Co-?op \\((\\d+)\\)$");
    // optional SkyBlock level, the name, then e.g. a symbol or "(Offline 12d)"
    private static final Pattern MEMBER_PATTERN = Pattern.compile("^(?:\\[\\d+] )?(\\w{1,16})(.*)$");
    private static final Pattern OFFLINE_PATTERN = Pattern.compile("\\(Offline(?: ([^)]*))?\\)");
    /** Used when the tab list shows no profile name. */
    private static final String UNKNOWN_PROFILE = "unknown";

    @Getter
    private static final CoopMemberTracker instance = new CoopMemberTracker();

    /** When the widget was last seen in this session, 0 if not yet. */
    private long liveAt;

    private CoopMemberTracker() {
    }

    /** Re-reads the tab list and caches the members if the widget is shown. */
    public void refresh() {
        final Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null || client.player == null) {
            return;
        }
        final List<String> lines = new ArrayList<>();
        for (final PlayerInfo info : ((PlayerTabOverlayAccessor) client.gui.hud.getTabList()).skysens$getPlayerInfos()) {
            final Component displayName = info.getTabListDisplayName();
            final String line = displayName == null ? null : ChatFormatting.stripFormatting(displayName.getString());
            lines.add(line == null ? "" : line.trim());
        }
        final CoopRepository repository = RepositoryRegistry.COOP_REPOSITORY;
        final String profile = parseProfile(lines);
        if (profile != null) {
            repository.lastProfile = profile;
        }
        final List<Member> members = parseMembers(lines, client.player.getName().getString());
        if (members != null) {
            final Coop coop = repository.profiles.computeIfAbsent(profile == null ? UNKNOWN_PROFILE : profile, key -> new Coop());
            coop.members = members;
            coop.lastSeenAt = System.currentTimeMillis();
            this.liveAt = coop.lastSeenAt;
        }
    }

    /** The cached co-op of the current profile, null if its members were never read. */
    public @Nullable Coop getCoop() {
        final CoopRepository repository = RepositoryRegistry.COOP_REPOSITORY;
        final Coop coop = repository.profiles.get(repository.lastProfile == null ? UNKNOWN_PROFILE : repository.lastProfile);
        return coop != null ? coop : repository.profiles.get(UNKNOWN_PROFILE);
    }

    /** True if the widget is in the tab list right now, so the online status is current. */
    public boolean isLive() {
        return this.liveAt != 0 && System.currentTimeMillis() - this.liveAt < 2000;
    }

    static @Nullable String parseProfile(@NotNull List<String> lines) {
        for (final String line : lines) {
            final Matcher matcher = PROFILE_PATTERN.matcher(line);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }
        return null;
    }

    /** The members below the "Coop (n)" header without {@code self}, or null if there is no such header. */
    static @Nullable List<Member> parseMembers(@NotNull List<String> lines, @NotNull String self) {
        for (int i = 0; i < lines.size(); i++) {
            final Matcher header = HEADER_PATTERN.matcher(lines.get(i));
            if (!header.matches()) {
                continue;
            }
            final int count = Integer.parseInt(header.group(1));
            final List<Member> members = new ArrayList<>();
            for (int j = i + 1; j < lines.size() && j <= i + count; j++) {
                final Matcher member = MEMBER_PATTERN.matcher(lines.get(j));
                if (!member.matches()) {
                    break;
                }
                final String name = member.group(1);
                if (name.equalsIgnoreCase(self)) {
                    continue;
                }
                final Matcher offline = OFFLINE_PATTERN.matcher(member.group(2));
                members.add(offline.find()
                        ? new Member(name, false, offline.group(1))
                        : new Member(name, true, null));
            }
            return members;
        }
        return null;
    }

}
