package de.vantrex.skysens.client.features.volcano;

import de.vantrex.skysens.client.chatcommand.ChatChannel;
import de.vantrex.skysens.client.skyblock.party.PartyWarpService;
import de.vantrex.skysens.client.util.Brand;
import de.vantrex.skysens.client.util.ChatUtil;
import de.vantrex.skysens.client.util.ClientUtil;
import lombok.experimental.UtilityClass;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The requesting side of {@code !warpme}: auto-accepts the party invite of the player who announced a CATACLYSMIC
 * eruption, see {@link PartyWarpService#acceptInviteFrom(String, long)}. Only acts within
 * {@value #ANNOUNCEMENT_MAX_AGE_MS} ms of such an announcement:
 * <ul>
 *     <li>A {@code !warpme} sent to the announcer via /msg ("To [MVP+] Name: !warpme") accepts their invite.</li>
 *     <li>A {@code !warpme} in party or co-op chat accepts the announcer's invite.</li>
 *     <li>When the announcer queues us ("you're #2 in line"), the invite is accepted for longer.</li>
 * </ul>
 * Client thread only.
 */
@UtilityClass
class WarpMeRequests {

    private static final long ANNOUNCEMENT_MAX_AGE_MS = 5 * 60_000;
    private static final Pattern OWN_PRIVATE_REQUEST = Pattern.compile("^To (?:\\[[^]]+] )?(\\w{1,16})(?: [^\\w\\s:]+)?: !warpme\\b.*$");
    private static final String BRAND = Pattern.quote(Brand.uncoloredPrefix().trim());
    /** The text of a cataclysmic eruption announcement that offers !warpme, see {@link VolcanoEruptionFeature}. */
    private static final Pattern ANNOUNCEMENT = Pattern.compile("^" + BRAND
            + " CATACLYSMIC volcano eruption on \\S+ - (?:type|reply) !warpme to get warped(?:" + ChatUtil.ANTI_SPAM_SUFFIX_REGEX + ")?$");
    /** The reply of {@link PartyWarpService} when our warp is queued. */
    private static final Pattern QUEUED = Pattern.compile("^" + BRAND + " Warping someone else right now, you're #(\\d+) in line(?:" + ChatUtil.ANTI_SPAM_SUFFIX_REGEX + ")?$");

    private @Nullable String lastAnnouncer;
    private long lastAnnouncedAt;

    /** Whether we saw a cataclysmic eruption announcement recently, only then {@code !warpme} is active for us. */
    boolean hasRecentAnnouncement() {
        return lastAnnouncer != null && System.currentTimeMillis() - lastAnnouncedAt <= ANNOUNCEMENT_MAX_AGE_MS;
    }

    /** Called for every chat message, formatting stripped. */
    void onChatMessage(@NotNull String message) {
        // idle until a cataclysmic eruption is announced
        if (!hasRecentAnnouncement() && !message.contains("CATACLYSMIC volcano eruption")) {
            return;
        }
        final Matcher own = OWN_PRIVATE_REQUEST.matcher(message);
        if (own.matches()) {
            if (isLastAnnouncer(own.group(1))) {
                PartyWarpService.getInstance().acceptInviteFrom(own.group(1));
            }
            return;
        }
        for (final ChatChannel channel : ChatChannel.values()) {
            final Matcher matcher = channel.getPattern().matcher(message);
            if (!matcher.matches()) {
                continue;
            }
            final String sender = matcher.group(1);
            final String text = matcher.group(2).trim();
            if (ANNOUNCEMENT.matcher(text).matches() && !isSelf(sender)) {
                lastAnnouncer = sender;
                lastAnnouncedAt = System.currentTimeMillis();
                return;
            }
            final Matcher queued = QUEUED.matcher(text);
            if (queued.matches() && isLastAnnouncer(sender)) {
                // every warp before ours can take up to a full invite timeout
                final long position = Long.parseLong(queued.group(1));
                PartyWarpService.getInstance().acceptInviteFrom(sender, (position + 1) * PartyWarpService.INVITE_TIMEOUT_MS);
            }
            return;
        }
    }

    /** We sent !warpme in party or co-op chat: accept the invite of the last announcer. */
    void onOwnChannelRequest() {
        if (!hasRecentAnnouncement()) {
            ClientUtil.sendDebug("!warpme: no recent cataclysmic eruption announcement, not auto-accepting an invite");
            return;
        }
        PartyWarpService.getInstance().acceptInviteFrom(lastAnnouncer);
    }

    private boolean isLastAnnouncer(String name) {
        return hasRecentAnnouncement() && name.equalsIgnoreCase(lastAnnouncer);
    }

    private static boolean isSelf(String name) {
        final var player = Minecraft.getInstance().player;
        return player != null && player.getName().getString().equalsIgnoreCase(name);
    }

}
