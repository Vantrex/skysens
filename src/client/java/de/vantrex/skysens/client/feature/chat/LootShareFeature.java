package de.vantrex.skysens.client.feature.chat;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.chatcommand.ChatChannel;
import de.vantrex.skysens.client.chatcommand.ChatCommandService;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.chat.LootShareAccordion;
import de.vantrex.skysens.client.enums.DropTierEnum;
import de.vantrex.skysens.client.feature.AfterTickFeature;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import de.vantrex.skysens.client.price.ItemPriceService;
import de.vantrex.skysens.client.util.ClientUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Posts our rare drops to co-op chat, e.g. "RARE DROP! Judgement Core (+250% Magic Find)". */
@SkySensFeature
public class LootShareFeature implements GameMessageListeningFeature, AfterTickFeature {

    /** Anchored at the start, so chat messages ("Co-op > X: RARE DROP! ...") never match. */
    private static final Pattern DROP_PATTERN = Pattern.compile(
            "^((?:VERY |CRAZY )?RARE DROP!|INSANE DROP!|PRAY TO RNGESUS DROP!|PET DROP!) (.+)$");
    private static final Pattern MAGIC_FIND_PATTERN = Pattern.compile("\\s*\\(\\+[\\d,.]+%[^)]*Magic Find\\)", Pattern.CASE_INSENSITIVE);
    /** "2x Item", "Item x2", optionally wrapped in parentheses. */
    private static final Pattern ITEM_PATTERN = Pattern.compile("^\\(?(?:(\\d{1,5})x )?(.+?)(?: x(\\d{1,5}))?\\)?$");
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
        if (!matcher.matches() || !this.shouldShareTier(matcher.group(1))) {
            return;
        }
        final LootShareAccordion config = SkysensConfig.getInstance().chatCommandsCategory.lootShare;
        final String withoutMagicFind = MAGIC_FIND_PATTERN.matcher(matcher.group(2)).replaceAll("").trim();
        if (!this.isWorthEnough(withoutMagicFind, config)) {
            return;
        }
        String drop = config.includeMagicFind ? matcher.group(2) : withoutMagicFind;
        // Hypixel rejects some symbols (e.g. the Magic Find star) in player messages
        drop = drop.replaceAll("[^\\x20-\\x7E]", "").replaceAll("\\s{2,}", " ").trim();
        ChatCommandService.getInstance().send(ChatChannel.COOP, matcher.group(1) + " " + drop);
    }

    @Override
    public void afterTick() {
        if (SkysensConfig.getInstance().chatCommandsCategory.lootShare.minimumPriceMillions > 0) {
            ItemPriceService.getInstance().prefetch();
        }
    }

    private boolean shouldShareTier(String prefix) {
        final LootShareAccordion config = SkysensConfig.getInstance().chatCommandsCategory.lootShare;
        if (prefix.equals(PET_DROP)) {
            return config.petDrops;
        }
        final DropTierEnum tier = DropTierEnum.fromPrefix(prefix);
        return tier != null && tier.ordinal() >= config.minimumTier.ordinal();
    }

    private boolean isWorthEnough(String item, LootShareAccordion config) {
        if (config.minimumPriceMillions <= 0) {
            return true;
        }
        final Matcher matcher = ITEM_PATTERN.matcher(item);
        final String name = matcher.matches() ? matcher.group(2) : item;
        final int amount = !matcher.matches() ? 1
                : matcher.group(1) != null ? Integer.parseInt(matcher.group(1))
                : matcher.group(3) != null ? Integer.parseInt(matcher.group(3))
                : 1;
        final OptionalDouble price = ItemPriceService.getInstance().getPrice(name);
        if (price.isEmpty()) {
            ClientUtil.sendDebug("Loot share: no price for " + name);
            return config.shareUnknownPrice;
        }
        return price.getAsDouble() * amount >= config.minimumPriceMillions * 1_000_000.0;
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().chatCommandsCategory.lootShare.enabled;
    }

}
