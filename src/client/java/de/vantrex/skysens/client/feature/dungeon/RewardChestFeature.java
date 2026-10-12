package de.vantrex.skysens.client.feature.dungeon;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.chatcommand.ChatChannel;
import de.vantrex.skysens.client.chatcommand.ChatCommandService;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.dungeon.ChestLootShareAccordion;
import de.vantrex.skysens.client.config.categories.dungeon.DungeonCategory;
import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.feature.AfterTickFeature;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import de.vantrex.skysens.client.price.ItemPriceService;
import de.vantrex.skysens.client.profit.ProfitService;
import de.vantrex.skysens.client.repository.ProfitRepository;
import de.vantrex.skysens.client.repository.RepositoryRegistry;
import de.vantrex.skysens.client.service.LocationService;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.ItemIdUtil;
import de.vantrex.skysens.client.util.NumberUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tracks and shares the loot of opened dungeon and Kuudra reward chests.
 * <p>
 * Hypixel announces an opened chest in chat, one message with the chest type and the rewards:
 * <pre>
 *   BEDROCK CHEST REWARDS
 *     RARE REWARD! Recombobulator 3000
 *     Enchanted Book (Wisdom II)
 *     Undead Essence x102
 * </pre>
 * The message has no cost, so the cost is read from the "Open Reward Chest" button (slot 31) while the
 * chest is open, like SkyHanni and Odin do: "100,000 Coins", "FREE" and/or a key.
 * <p>
 * The profit of a dungeon chest also counts for its floor in {@link DungeonRunStats}: the floor of the instance
 * for chests opened in the run, or the floor of the run opened at Croesus ("Master Catacombs - Floor VII").
 */
@SkySensFeature
public class RewardChestFeature implements GameMessageListeningFeature, AfterTickFeature {

    private static final Pattern REWARDS_HEADER = Pattern.compile("^\\s*(\\w+) CHEST REWARDS\\s*$");
    /** Boss room and Croesus chests ("Bedrock Chest", "Bedrock"), Kuudra chests ("Paid Chest Chest"). */
    private static final Pattern CHEST_SCREEN = Pattern.compile("^(Wood|Iron|Gold|Diamond|Emerald|Obsidian|Bedrock|Free|Paid)(?: Chest){0,2}$");
    /** A run's chests at Croesus. */
    private static final Pattern CROESUS_RUN_SCREEN = Pattern.compile("^(Master )?Catacombs - Floor ([IVX]+)$");
    private static final int OPEN_BUTTON_SLOT = 31;
    /** A cost seen longer ago than this belongs to another chest. */
    private static final long COST_MAX_AGE_MS = 2 * 60 * 1000;
    private static final int MAX_SHARE_LENGTH = 200;

    private static final Pattern RARE_REWARD = Pattern.compile("^RARE REWARD! ");
    private static final Pattern COINS = Pattern.compile("^([\\d,]+) Coins$");
    private static final Pattern ESSENCE = Pattern.compile("^(\\w+) Essence x([\\d,]+)$");
    private static final Pattern BOOK = Pattern.compile("^Enchanted Book \\((.+) ([IVXLC]+)\\)$");
    private static final Pattern SHARD = Pattern.compile("^(.+) Shard(?: x([\\d,]+))?$");
    private static final Pattern AMOUNT = Pattern.compile("^(.+?) x([\\d,]+)$");

    private final Map<String, SeenCost> costByChest = new HashMap<>();
    private final Map<String, SeenFloor> floorByChest = new HashMap<>();
    /** The floor of the run last opened at Croesus. */
    private @Nullable SeenFloor croesusFloor;
    private final ProfitRepository.DungeonChestStats session = new ProfitRepository.DungeonChestStats();

    /** Cost parts seen on the open button: coins, and keys that are priced once the chest is opened. */
    private record SeenCost(double coins, List<String> keys, long seenAt) {
    }

    /** The floor ("M7") a chest belongs to. */
    private record SeenFloor(String floor, long seenAt) {
    }

    private record Reward(String name, double value) {
    }

    /** A rewards message still receiving reward lines. */
    private record PendingChest(String chest, List<Reward> rewards, long startedAt) {
    }

    /** All lines arrive at once; if the closing separator never comes, finish after this. */
    private static final long PENDING_TIMEOUT_MS = 1000;
    private @Nullable PendingChest pending;

    @Override
    public boolean isActive() {
        final DungeonCategory config = SkysensConfig.getInstance().dungeonCategory;
        return config.chestProfit.enabled || config.chestLootShare.enabled;
    }

    // --- cost from the open chest GUI ---

    @Override
    public void afterTick() {
        if (this.pending != null && System.currentTimeMillis() - this.pending.startedAt() > PENDING_TIMEOUT_MS) {
            this.finishPending();
        }
        final Screen screen = Minecraft.getInstance().gui.screen();
        final String screenTitle = screen == null ? null : screen.getTitle().getString().trim();
        final Matcher croesusRun = screenTitle == null ? null : CROESUS_RUN_SCREEN.matcher(screenTitle);
        if (croesusRun != null && croesusRun.matches()) {
            this.croesusFloor = new SeenFloor((croesusRun.group(1) == null ? "F" : "M")
                    + ItemIdUtil.romanToInt(croesusRun.group(2)), System.currentTimeMillis());
        }
        final Matcher title = screenTitle == null ? null : CHEST_SCREEN.matcher(screenTitle);
        final boolean chestOpen = title != null && title.matches();
        if (chestOpen || isNearRewardChests()) {
            // load prices before the first chest is opened; only here, not everywhere on SkyBlock
            ItemPriceService.getInstance().prefetch();
            ItemPriceService.getInstance().prefetchOwn();
        }
        if (chestOpen) {
            final SeenFloor floor = this.currentChestFloor();
            if (floor != null) {
                this.floorByChest.put(title.group(1).toUpperCase(Locale.ROOT), floor);
            }
        }
        if (!chestOpen || !(screen instanceof AbstractContainerScreen<?> container)
                || container.getMenu().slots.size() <= OPEN_BUTTON_SLOT) {
            return;
        }
        final Slot button = container.getMenu().slots.get(OPEN_BUTTON_SLOT);
        final SeenCost cost = readCost(button.getItem());
        if (cost != null) {
            this.costByChest.put(title.group(1).toUpperCase(Locale.ROOT), cost);
        }
    }

    private static boolean isNearRewardChests() {
        final SkyblockLocationEnum location = LocationService.getInstance().getCurrentLocation();
        // dungeon and Kuudra runs, the Dungeon Hub (Croesus) and the Crimson Isle
        return location != null && location.isInstance()
                || location == SkyblockLocationEnum.DUNGEON_HUB || location == SkyblockLocationEnum.CRIMSON_ISLE;
    }

    /** The floor of a chest open right now: the instance's floor in a run, else the run last opened at Croesus. */
    private @Nullable SeenFloor currentChestFloor() {
        if (LocationService.getInstance().getCurrentLocation() == SkyblockLocationEnum.CATACOMBS) {
            final String floor = DungeonRunStats.getInstance().getCurrentFloor();
            return floor == null ? null : new SeenFloor(floor, System.currentTimeMillis());
        }
        final SeenFloor croesus = this.croesusFloor;
        return croesus != null && System.currentTimeMillis() - croesus.seenAt() <= COST_MAX_AGE_MS ? croesus : null;
    }

    /** Cost parts from the lore of the open button, or null if the lore has no cost (yet). Runs every tick, so no pricing here. */
    static @Nullable SeenCost readCost(ItemStack button) {
        final ItemLore lore = button.get(DataComponents.LORE);
        return lore == null ? null : readCost(lore.lines().stream().map(line -> strip(line.getString())).toList());
    }

    static @Nullable SeenCost readCost(List<String> lore) {
        boolean found = false;
        double coins = 0;
        final List<String> keys = new ArrayList<>();
        for (final String text : lore) {
            final Matcher coinsLine = COINS.matcher(text);
            if (coinsLine.matches()) {
                coins += parseNumber(coinsLine.group(1));
                found = true;
            } else if (text.equals("FREE")) {
                found = true;
            } else if (text.endsWith(" Key") && !text.startsWith("Requires")) {
                // Dungeon Chest Key, Kuudra keys
                keys.add(text);
                found = true;
            }
        }
        return found ? new SeenCost(coins, keys, System.currentTimeMillis()) : null;
    }

    // --- the rewards message ---

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final String text = ChatFormatting.stripFormatting(message.getString());
        if (text == null || (this.pending == null && !text.contains("CHEST REWARDS"))) {
            return;
        }
        // Hypixel sends the header and every reward as separate messages, closed by a separator line
        for (final String rawLine : text.split("\n")) {
            this.handleLine(rawLine);
        }
    }

    private void handleLine(String rawLine) {
        final Matcher header = REWARDS_HEADER.matcher(rawLine);
        if (header.matches()) {
            this.finishPending();
            this.pending = new PendingChest(header.group(1).toUpperCase(Locale.ROOT), new ArrayList<>(), System.currentTimeMillis());
            return;
        }
        if (this.pending == null) {
            return;
        }
        // drop "RARE REWARD!", the separator, dungeon stars ("Terror Leggings ✪✪") and chat-mod repeat counters (" (2)")
        final String name = RARE_REWARD.matcher(rawLine.trim()).replaceFirst("")
                .replaceAll("[^\\x20-\\x7E]", "")
                .replaceAll("(?:^| )\\(\\d+\\)$", "")
                .trim();
        // rewards are indented; the separator or any other message ends the list
        if (name.isEmpty() || name.startsWith("---") || !rawLine.startsWith("  ")) {
            this.finishPending();
            return;
        }
        final PendingChest chest = this.pending;
        chest.rewards().add(new Reward(name, value(name)));
    }

    private void finishPending() {
        final PendingChest chest = this.pending;
        this.pending = null;
        if (chest != null) {
            this.onChestOpened(chest.chest(), chest.rewards());
        }
    }

    private void onChestOpened(String chest, List<Reward> rewards) {
        final SeenCost seen = this.costByChest.remove(chest);
        final boolean costKnown = seen != null && System.currentTimeMillis() - seen.seenAt() <= COST_MAX_AGE_MS;
        final double cost = costKnown
                ? seen.coins() + seen.keys().stream().mapToDouble(key -> ItemPriceService.getInstance().getPrice(key).orElse(0)).sum()
                : 0;
        final double revenue = rewards.stream().mapToDouble(Reward::value).sum();
        final double profit = revenue - cost;
        final String chestName = displayName(chest);
        ClientUtil.sendDebug("Opened " + chestName + ": revenue " + revenue + ", cost " + (costKnown ? cost : "unknown") + ", " + rewards);

        final DungeonCategory config = SkysensConfig.getInstance().dungeonCategory;
        if (config.chestProfit.enabled) {
            this.session.add(revenue, cost);
            RepositoryRegistry.PROFIT_REPOSITORY.dungeonChests.add(revenue, cost);
            // the total stays tracked as "Dungeons", the floor's share comes on top for the run stats
            ProfitService.getInstance().record(isKuudra(chest) ? ProfitService.KUUDRA : ProfitService.DUNGEONS, profit);
            final SeenFloor floor = this.floorByChest.remove(chest);
            if (!isKuudra(chest) && floor != null && System.currentTimeMillis() - floor.seenAt() <= COST_MAX_AGE_MS
                    && config.runStats.enabled) {
                DungeonRunStats.getInstance().recordChest(floor.floor(), profit);
            }
            if (config.chestProfit.chatSummary) {
                ClientUtil.sendPrefixed("§7" + chestName + ": " + coins(profit)
                        + " §8(" + NumberUtil.formatCoins(revenue) + " - " + (costKnown ? NumberUtil.formatCoins(cost) : "? cost") + ")"
                        + " §7| Session: " + coins(this.session.profit()) + " §7over §f" + this.session.chests + " §7chests");
            }
        }
        if (config.chestLootShare.enabled) {
            this.share(chestName, rewards, profit, config.chestLootShare);
        }
    }

    private void share(String chestName, List<Reward> rewards, double profit, ChestLootShareAccordion config) {
        if (profit < config.minimumProfitMillions * 1_000_000.0) {
            return;
        }
        final String suffix = config.showProfit ? " - " + NumberUtil.formatCoins(profit) + " profit" : "";
        final List<Reward> sorted = new ArrayList<>(rewards);
        sorted.sort(Comparator.comparingDouble(Reward::value).reversed());
        final StringBuilder message = new StringBuilder(chestName).append(':');
        int shown = 0;
        for (final Reward reward : sorted) {
            final String name = reward.name().replaceAll("^Enchanted Book \\((.+)\\)$", "$1");
            if (message.length() + name.length() + suffix.length() + 12 > MAX_SHARE_LENGTH) {
                break;
            }
            message.append(shown == 0 ? " " : ", ").append(name);
            shown++;
        }
        if (shown < sorted.size()) {
            message.append(" +").append(sorted.size() - shown).append(" more");
        }
        message.append(suffix);
        for (final ChatChannel channel : config.channel.getChannels()) {
            ChatCommandService.getInstance().send(channel, message.toString());
        }
    }

    // --- pricing ---

    /** Value of one reward line; 0 if unknown. */
    private static double value(String reward) {
        final ItemPriceService prices = ItemPriceService.getInstance();
        final Matcher coins = COINS.matcher(reward);
        if (coins.matches()) {
            return parseNumber(coins.group(1));
        }
        final Matcher essence = ESSENCE.matcher(reward);
        if (essence.matches()) {
            return prices.getPriceById("ESSENCE_" + essence.group(1).toUpperCase(Locale.ROOT)).orElse(0) * parseNumber(essence.group(2));
        }
        final Matcher book = BOOK.matcher(reward);
        if (book.matches()) {
            return prices.getBookPrice(book.group(1), ItemIdUtil.romanToInt(book.group(2))).orElse(0);
        }
        final Matcher shard = SHARD.matcher(reward);
        if (shard.matches()) {
            final double amount = shard.group(2) == null ? 1 : parseNumber(shard.group(2));
            final OptionalDouble price = prices.getPriceById("SHARD_" + ItemIdUtil.toId(shard.group(1)));
            if (price.isPresent()) {
                return price.getAsDouble() * amount;
            }
        }
        final Matcher amount = AMOUNT.matcher(reward);
        if (amount.matches()) {
            return prices.getPrice(amount.group(1)).orElse(0) * parseNumber(amount.group(2));
        }
        return prices.getPrice(reward).orElse(0);
    }

    // --- tracker output ---

    public ProfitRepository.DungeonChestStats getSession() {
        return this.session;
    }

    public static @NotNull String summary(String label, ProfitRepository.DungeonChestStats stats) {
        return "§7Chest profit (" + label + "): " + coins(stats.profit())
                + " §7over §f" + stats.chests + " §7chests §8(revenue " + NumberUtil.formatCoins(stats.revenue)
                + ", cost " + NumberUtil.formatCoins(stats.cost) + ")";
    }

    private static String coins(double amount) {
        return (amount >= 0 ? "§a+" : "§c-") + NumberUtil.formatCoins(Math.abs(amount));
    }

    private static boolean isKuudra(String chest) {
        return chest.equals("FREE") || chest.equals("PAID");
    }

    private static String displayName(String chest) {
        return chest.charAt(0) + chest.substring(1).toLowerCase(Locale.ROOT) + " Chest";
    }

    private static String strip(String text) {
        final String stripped = ChatFormatting.stripFormatting(text);
        return (stripped == null ? text : stripped).trim();
    }

    private static double parseNumber(String number) {
        return Double.parseDouble(number.replace(",", ""));
    }

}
