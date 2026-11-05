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
 * The requesting side of {@code !warpme}: auto-accepts the party invite of the player who found the eruption, see
 * {@link PartyWarpService#acceptInviteFrom(String)}.
 * <ul>
 *     <li>A {@code !warpme} sent via /msg ("To [MVP+] Name: !warpme") accepts that player's invite.</li>
 *     <li>A {@code !warpme} in party or co-op chat accepts the invite of the player whose eruption announcement we saw
 *     last, within {@value #ANNOUNCEMENT_MAX_AGE_MS} ms.</li>
 * </ul>
 * Client thread only.
 */
@UtilityClass
class WarpMeRequests {

    private static final long ANNOUNCEMENT_MAX_AGE_MS = 5 * 60_000;
    private static final Pattern OWN_PRIVATE_REQUEST = Pattern.compile("^To (?:\\[[^]]+] )?(\\w{1,16})(?: [^\\w\\s:]+)?: !warpme\\b.*$");
    /** The text of an eruption announcement that offers !warpme, see {@link VolcanoEruptionFeature}. */
    private static final Pattern ANNOUNCEMENT = Pattern.compile("^" + Pattern.quote(Brand.uncoloredPrefix().trim())
            + " [A-Z]+ volcano eruption on \\S+ - (?:type|reply) !warpme to get warped(?:" + ChatUtil.ANTI_SPAM_SUFFIX_REGEX + ")?$");

    private @Nullable String lastAnnouncer;
    private long lastAnnouncedAt;

    /** Called for every chat message, formatting stripped. */
    void onChatMessage(@NotNull String message) {
        final Matcher own = OWN_PRIVATE_REQUEST.matcher(message);
        if (own.matches()) {
            PartyWarpService.getInstance().acceptInviteFrom(own.group(1));
            return;
        }
        for (final ChatChannel channel : ChatChannel.values()) {
            final Matcher matcher = channel.getPattern().matcher(message);
            if (matcher.matches()) {
                if (ANNOUNCEMENT.matcher(matcher.group(2).trim()).matches() && !isSelf(matcher.group(1))) {
                    lastAnnouncer = matcher.group(1);
                    lastAnnouncedAt = System.currentTimeMillis();
                }
                return;
            }
        }
    }

    /** We sent !warpme in party or co-op chat: accept the invite of the last announcer. */
    void onOwnChannelRequest() {
        if (lastAnnouncer == null || System.currentTimeMillis() - lastAnnouncedAt > ANNOUNCEMENT_MAX_AGE_MS) {
            ClientUtil.sendDebug("!warpme: no recent eruption announcement, not auto-accepting an invite");
            return;
        }
        PartyWarpService.getInstance().acceptInviteFrom(lastAnnouncer);
    }

    private static boolean isSelf(String name) {
        final var player = Minecraft.getInstance().player;
        return player != null && player.getName().getString().equalsIgnoreCase(name);
    }

}
