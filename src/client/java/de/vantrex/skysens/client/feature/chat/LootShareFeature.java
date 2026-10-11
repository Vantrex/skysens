package de.vantrex.skysens.client.feature.chat;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.chatcommand.ChatChannel;
import de.vantrex.skysens.client.chatcommand.ChatCommandService;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.chat.LootShareAccordion;
import de.vantrex.skysens.client.enums.DropTierEnum;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Posts our rare drops to co-op chat, e.g. "RARE DROP! Judgement Core (+250% Magic Find)". */
@SkySensFeature
public class LootShareFeature implements GameMessageListeningFeature {

    /** Anchored at the start, so chat messages ("Co-op > X: RARE DROP! ...") never match. */
    private static final Pattern DROP_PATTERN = Pattern.compile(
            "^((?:VERY |CRAZY )?RARE DROP!|INSANE DROP!|PRAY TO RNGESUS DROP!|PET DROP!) (.+)$");
    private static final Pattern MAGIC_FIND_PATTERN = Pattern.compile("\\s*\\(\\+[\\d,.]+%[^)]*Magic Find\\)", Pattern.CASE_INSENSITIVE);
    private static final String PET_DROP = "PET DROP!";

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final String text = ChatFormatting.stripFormatting(message.getString());
        if (text == null) {
            return;
        }
        final Matcher matcher = DROP_PATTERN.matcher(text.trim());
        if (!matcher.matches() || !this.shouldShare(matcher.group(1))) {
            return;
        }
        final LootShareAccordion config = SkysensConfig.getInstance().chatCommandsCategory.lootShare;
        String drop = matcher.group(2);
        if (!config.includeMagicFind) {
            drop = MAGIC_FIND_PATTERN.matcher(drop).replaceAll("");
        }
        // Hypixel rejects some symbols (e.g. the Magic Find star) in player messages
        drop = drop.replaceAll("[^\\x20-\\x7E]", "").replaceAll("\\s{2,}", " ").trim();
        ChatCommandService.getInstance().send(ChatChannel.COOP, matcher.group(1) + " " + drop);
    }

    private boolean shouldShare(String prefix) {
        final LootShareAccordion config = SkysensConfig.getInstance().chatCommandsCategory.lootShare;
        if (prefix.equals(PET_DROP)) {
            return config.petDrops;
        }
        final DropTierEnum tier = DropTierEnum.fromPrefix(prefix);
        return tier != null && tier.ordinal() >= config.minimumTier.ordinal();
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().chatCommandsCategory.lootShare.enabled;
    }

}
