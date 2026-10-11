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
import de.vantrex.skysens.client.profit.ProfitService;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.NumberUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Posts our rare drops to co-op chat, e.g. "RARE DROP! Judgement Core (+250% Magic Find)", and records their
 * value for the profit tracker.
 */
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
        if (!matcher.matches()) {
            return;
        }
        final LootShareAccordion config = SkysensConfig.getInstance().chatCommandsCategory.lootShare;
        final String withoutMagicFind = MAGIC_FIND_PATTERN.matcher(matcher.group(2)).replaceAll("").trim();
        final OptionalDouble value = needsPrices(config) ? this.dropValue(withoutMagicFind) : OptionalDouble.empty();
        // every drop counts for the profit tracker, the share filters below don't apply to it
        if (value.isPresent()) {
            ProfitService.getInstance().record(ProfitService.detectActivity(), value.getAsDouble());
        }
        if (!config.enabled || !this.shouldShareTier(matcher.group(1)) || !this.isWorthEnough(value, config)) {
            return;
        }
        String drop = config.includeMagicFind ? matcher.group(2) : withoutMagicFind;
        // Hypixel rejects some symbols (e.g. the Magic Find star) in player messages
        drop = drop.replaceAll("[^\\x20-\\x7E]", "").replaceAll("\\s{2,}", " ").trim();
        if (config.showPrice && value.isPresent()) {
            drop += " - " + NumberUtil.formatCoins(value.getAsDouble()) + " coins";
        }
        ChatCommandService.getInstance().send(ChatChannel.COOP, matcher.group(1) + " " + drop);
    }

    @Override
    public void afterTick() {
        if (needsPrices(SkysensConfig.getInstance().chatCommandsCategory.lootShare)) {
            ItemPriceService.getInstance().prefetch();
        }
    }

    private static boolean needsPrices(LootShareAccordion config) {
        return isTrackingProfit() || (config.enabled && (config.showPrice || config.minimumPriceMillions > 0));
    }

    private static boolean isTrackingProfit() {
        return SkysensConfig.getInstance().generalCategory.profitTracker.enabled;
    }

    private boolean shouldShareTier(String prefix) {
        final LootShareAccordion config = SkysensConfig.getInstance().chatCommandsCategory.lootShare;
        if (prefix.equals(PET_DROP)) {
            return config.petDrops;
        }
        final DropTierEnum tier = DropTierEnum.fromPrefix(prefix);
        return tier != null && tier.ordinal() >= config.minimumTier.ordinal();
    }

    /** Total value of the drop (price times amount), empty if the price is unknown. */
    private OptionalDouble dropValue(String item) {
        final Matcher matcher = ITEM_PATTERN.matcher(item);
        final String name = matcher.matches() ? matcher.group(2) : item;
        final int amount = !matcher.matches() ? 1
                : matcher.group(1) != null ? Integer.parseInt(matcher.group(1))
                : matcher.group(3) != null ? Integer.parseInt(matcher.group(3))
                : 1;
        final OptionalDouble price = ItemPriceService.getInstance().getPrice(name);
        if (price.isEmpty()) {
            ClientUtil.sendDebug("Loot share: no price for " + name);
            return price;
        }
        return OptionalDouble.of(price.getAsDouble() * amount);
    }

    private boolean isWorthEnough(OptionalDouble value, LootShareAccordion config) {
        if (config.minimumPriceMillions <= 0) {
            return true;
        }
        if (value.isEmpty()) {
            return config.shareUnknownPrice;
        }
        return value.getAsDouble() >= config.minimumPriceMillions * 1_000_000.0;
    }

    @Override
    public boolean isActive() {
        // also runs without sharing, to feed the profit tracker
        return SkysensConfig.getInstance().chatCommandsCategory.lootShare.enabled || isTrackingProfit();
    }

}
