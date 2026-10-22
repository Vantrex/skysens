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
import de.vantrex.skysens.client.util.ItemIdUtil;
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

    /**
     * Anchored at the start, so chat messages (i.e. "Co-op/Party > X: RARE DROP! ...") never match. Leaves out the " (2)"
     * that chat stacking appends to repeated messages.
     */
    private static final Pattern DROP_PATTERN = Pattern.compile(
            "^((?:VERY |CRAZY )?RARE DROP!|INSANE DROP!|PRAY TO RNGESUS DROP!|PET DROP!) (.+?)(?: \\(\\d+\\))?$");
    /** "(+323% ✯ Magic Find)", some drops leave out the "%": "(+309 ✯ Magic Find)". */
    private static final Pattern MAGIC_FIND_PATTERN = Pattern.compile("\\s*\\(\\+[\\d,.]+%?[^)]*Magic Find\\)", Pattern.CASE_INSENSITIVE);
    /** "2x Item", "Item x2". */
    private static final Pattern ITEM_PATTERN = Pattern.compile("^(?:(\\d{1,5})x )?(.+?)(?: x(\\d{1,5}))?$");
    private static final Pattern BOOK_PATTERN = Pattern.compile("^Enchanted Book \\((.+) ([IVXLC]+)\\)$");
    /** "Fire Aspect III", an enchanted book named by its enchant only. */
    private static final Pattern ENCHANT_PATTERN = Pattern.compile("^(.+) ([IVXLC]+)$");
    private static final Pattern RUNE_PATTERN = Pattern.compile("^(.+) Rune ([IVXLC]+)$");
    private static final String PET_DROP = "PET DROP!";

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final String text = ChatFormatting.stripFormatting(message.getString());
        final Matcher matcher = DROP_PATTERN.matcher(text.trim());
        if (!matcher.matches()) {
            return;
        }
        final LootShareAccordion config = SkysensConfig.getInstance().chatCommandsCategory.lootShare;
        final String withoutMagicFind = MAGIC_FIND_PATTERN.matcher(matcher.group(2)).replaceAll("").trim();
        final String item = itemName(withoutMagicFind);
        final OptionalDouble value = needsPrices(config) ? dropValue(item) : OptionalDouble.empty();
        // every drop counts for the profit tracker, the share filters below don't apply to it
        if (value.isPresent()) {
            ProfitService.getInstance().record(ProfitService.detectActivity(), value.getAsDouble());
            this.rememberDrop(item);
        }
        final boolean worthSharing = value.isPresent()
                ? isWorthEnough(value.getAsDouble(), config)
                : shouldShareUnknownPrice(config);
        if (!config.enabled || !this.shouldShareTier(matcher.group(1)) || !worthSharing
                || (!config.shareRunes && RUNE_PATTERN.matcher(item).matches())) {
            return;
        }
        String drop = asciiOnly(config.includeMagicFind ? matcher.group(2) : withoutMagicFind);
        if (config.showPrice && value.isPresent()) {
            drop += " - " + NumberUtil.formatCoins(value.getAsDouble()) + " coins";
        }
        ChatCommandService.getInstance().send(ChatChannel.COOP, matcher.group(1) + " " + drop);
    }

    @Override
    public void afterTick() {
        if (needsPrices(SkysensConfig.getInstance().chatCommandsCategory.lootShare)) {
            ItemPriceService.getInstance().prefetch();
            // enchanted books and runes are priced by id from SkySens' own data, also while SkyHanni is installed
            ItemPriceService.getInstance().prefetchOwn();
        }
    }

    private static boolean needsPrices(LootShareAccordion config) {
        // sharing always needs the price, worthless drops are never shared
        return isTrackingProfit() || config.enabled;
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

    /** Tells the sack tracking about this drop, so the "[Sacks]" message about the same item isn't counted again. */
    private void rememberDrop(String item) {
        final Matcher matcher = ITEM_PATTERN.matcher(item);
        if (!matcher.matches() || BOOK_PATTERN.matcher(item).matches()) {
            SackProfitFeature.rememberDrop(item, 1);
            return;
        }
        final String amount = matcher.group(1) != null ? matcher.group(1) : matcher.group(3);
        SackProfitFeature.rememberDrop(matcher.group(2), amount != null ? Integer.parseInt(amount) : 1);
    }

    /**
     * Total value of the drop (price times amount), empty if the price is unknown. Also accepts "27x Item", so the
     * sack tracking can price its entries the same way.
     */
    static OptionalDouble dropValue(String item) {
        final OptionalDouble value = lookUpValue(item);
        if (value.isEmpty()) {
            ClientUtil.sendDebug("Loot share: no price for " + item);
        }
        return value;
    }

    private static OptionalDouble lookUpValue(String item) {
        final ItemPriceService prices = ItemPriceService.getInstance();
        final Matcher book = BOOK_PATTERN.matcher(item);
        if (book.matches()) {
            return prices.getBookPrice(book.group(1), ItemIdUtil.romanToInt(book.group(2)));
        }
        final Matcher rune = RUNE_PATTERN.matcher(item);
        if (rune.matches()) {
            return prices.getRunePrice(rune.group(1), ItemIdUtil.romanToInt(rune.group(2)));
        }
        final Matcher matcher = ITEM_PATTERN.matcher(item);
        final String name = matcher.matches() ? matcher.group(2) : item;
        final int amount = !matcher.matches() ? 1
                : matcher.group(1) != null ? Integer.parseInt(matcher.group(1))
                : matcher.group(3) != null ? Integer.parseInt(matcher.group(3))
                : 1;
        final OptionalDouble price = prices.getPrice(name);
        if (price.isEmpty()) {
            // slayer books drop as just the enchant, e.g. "VERY RARE DROP! (Fire Aspect III)"
            final Matcher enchant = ENCHANT_PATTERN.matcher(name);
            return enchant.matches()
                    ? prices.getBookPrice(enchant.group(1), ItemIdUtil.romanToInt(enchant.group(2)))
                    : price;
        }
        return OptionalDouble.of(price.getAsDouble() * amount);
    }

    /** Hypixel rejects some symbols (e.g. the Magic Find star) in player messages, and they break price lookups. */
    private static String asciiOnly(String text) {
        return text.replaceAll("[^\\x20-\\x7E]", "").replaceAll("\\s{2,}", " ").trim();
    }

    /** "(◆ Lavatears Rune I)" -> "Lavatears Rune I"; "Enchanted Book (Smoldering I)" stays as it is. */
    private static String itemName(String drop) {
        final String name = asciiOnly(drop);
        return name.startsWith("(") && name.endsWith(")") ? name.substring(1, name.length() - 1).trim() : name;
    }

    private static boolean isWorthEnough(double value, LootShareAccordion config) {
        // e.g. a bazaar item without sell offers has an insta-buy price of 0
        return value > 0 && value >= config.minimumPriceMillions * 1_000_000.0;
    }

    private static boolean shouldShareUnknownPrice(LootShareAccordion config) {
        return config.minimumPriceMillions <= 0 || config.shareUnknownPrice;
    }

    @Override
    public boolean isActive() {
        // also runs without sharing, to feed the profit tracker
        return SkysensConfig.getInstance().chatCommandsCategory.lootShare.enabled || isTrackingProfit();
    }

}
