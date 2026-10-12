package de.vantrex.skysens.client.feature.dungeon;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.config.categories.dungeon.ChestProfitAccordion;
import de.vantrex.skysens.client.enums.location.SkyblockLocationEnum;
import de.vantrex.skysens.client.feature.AfterTickFeature;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import de.vantrex.skysens.client.feature.SkySensFeature;
import de.vantrex.skysens.client.price.ItemPriceService;
import de.vantrex.skysens.client.profit.ProfitService;
import de.vantrex.skysens.client.service.LocationService;
import de.vantrex.skysens.client.util.ClientUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Counts NPC sales and salvaged essence for the profit tracker.
 * <ul>
 *     <li>"You sold Dungeon VI Potion x1 for 4,500 Coins!" adds the coins. Counted as "Dungeons" in the
 *     Dungeon Hub and dungeons, elsewhere (if allowed) under the island's activity.</li>
 *     <li>"You salvaged 3 items for:" is followed by separate messages like "   15x Spider Essence!". The essence
 *     is priced and counted as "Dungeons".</li>
 * </ul>
 * Both go through {@link ProfitService#record}, which SkyHanni's trackers don't overlap with (they have no
 * tracker for NPC sales or salvaging), and which adds the "Dungeons" activity to the overall profit also when
 * SkyHanni is the selected tracker. So nothing is counted twice.
 */
@SkySensFeature
public class DungeonNpcProfitFeature implements GameMessageListeningFeature, AfterTickFeature {

    private static final Pattern SOLD = Pattern.compile("^You sold .+ for ([\\d,]+) Coins?!(?: \\(\\d+\\))?$");
    private static final Pattern SALVAGED = Pattern.compile("^You salvaged \\d+ items? for:(?: \\(\\d+\\))?$");
    private static final Pattern ESSENCE = Pattern.compile("^\\s+([\\d,]+)x (\\w+) Essence!(?: \\(\\d+\\))?$");

    /** The essence lines come right after the header; later messages are not part of it. */
    private static final long SALVAGE_WINDOW_MS = 2000;
    private long salvageUntil;
    private boolean prefetched;

    @Override
    public boolean isActive() {
        final ChestProfitAccordion config = SkysensConfig.getInstance().dungeonCategory.chestProfit;
        return SkysensConfig.getInstance().generalCategory.profitTracker.enabled && (config.npcSales || config.salvage);
    }

    @Override
    public void afterTick() {
        // essence is priced from SkySens' own price data, load it before the first salvage
        if (!this.prefetched && this.isActive() && isInDungeonArea()) {
            this.prefetched = true;
            ItemPriceService.getInstance().prefetchOwn();
        }
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay || !this.isActive() || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final String text = ChatFormatting.stripFormatting(message.getString());
        if (text == null) {
            return;
        }
        final ChestProfitAccordion config = SkysensConfig.getInstance().dungeonCategory.chestProfit;
        for (final String line : text.split("\n")) {
            final String stripped = line.stripTrailing();
            final Matcher sold = SOLD.matcher(stripped.trim());
            if (sold.matches()) {
                if (config.npcSales) {
                    this.onSold(Double.parseDouble(sold.group(1).replace(",", "")), config);
                }
            } else if (SALVAGED.matcher(stripped.trim()).matches()) {
                this.salvageUntil = System.currentTimeMillis() + SALVAGE_WINDOW_MS;
            } else if (config.salvage && System.currentTimeMillis() <= this.salvageUntil) {
                final Matcher essence = ESSENCE.matcher(stripped);
                if (essence.matches()) {
                    this.onSalvaged(essence.group(2), Double.parseDouble(essence.group(1).replace(",", "")));
                }
            }
        }
    }

    private void onSold(double coins, ChestProfitAccordion config) {
        final boolean inDungeons = isInDungeonArea();
        if (config.npcSalesDungeonsOnly && !inDungeons) {
            return;
        }
        ClientUtil.sendDebug("NPC sale: " + coins + " coins");
        ProfitService.getInstance().record(inDungeons ? ProfitService.DUNGEONS : ProfitService.detectActivity(), coins);
    }

    private void onSalvaged(String type, double amount) {
        final OptionalDouble price = ItemPriceService.getInstance().getPriceById("ESSENCE_" + type.toUpperCase(Locale.ROOT));
        if (price.isEmpty()) {
            ClientUtil.sendDebug("Salvage: no price for " + type + " essence");
            return;
        }
        ProfitService.getInstance().record(ProfitService.DUNGEONS, price.getAsDouble() * amount);
    }

    private static boolean isInDungeonArea() {
        final SkyblockLocationEnum location = LocationService.getInstance().getCurrentLocation();
        return location == SkyblockLocationEnum.DUNGEON_HUB || location == SkyblockLocationEnum.CATACOMBS;
    }

}
