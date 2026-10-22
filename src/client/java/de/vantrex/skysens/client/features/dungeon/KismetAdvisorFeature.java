package de.vantrex.skysens.client.features.dungeon;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.dungeon.config.KismetAdvisorAccordion;
import de.vantrex.skysens.client.core.feature.AfterTickFeature;
import de.vantrex.skysens.client.core.feature.HandledScreenFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.skyblock.price.ItemPriceService;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.skyblock.item.ItemIdUtil;
import de.vantrex.skysens.client.util.NumberUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Advises whether to reroll an open dungeon reward chest with a Kismet Feather.
 * <p>
 * The rule is deliberately simple and transparent, there are no loot tables: the chest's profit is the priced
 * rewards in the GUI minus the open cost (coins and key, read from the "Open Reward Chest" button like
 * {@link RewardChestFeature}). Below the configured threshold the advice is to reroll, otherwise to keep.
 * The advice is computed once per tick and drawn on the "Reroll Chest" button and as a line above the chest.
 */
@SkySensFeature
public class KismetAdvisorFeature implements HandledScreenFeature, AfterTickFeature {

    /** Boss room and Croesus chests, like the title of the chests in {@link RewardChestFeature}. */
    private static final Pattern CHEST_SCREEN = Pattern.compile("^(Wood|Iron|Gold|Diamond|Emerald|Obsidian|Bedrock)(?: Chest){0,2}$");
    private static final int OPEN_BUTTON_SLOT = 31;
    /** The rewards sit in the middle row of the chest (guessed). */
    private static final int FIRST_REWARD_SLOT = 9;
    private static final int LAST_REWARD_SLOT = 17;
    private static final String KISMET_ID = "KISMET_FEATHER";
    private static final String REROLL_BUTTON = "Reroll Chest";

    private static final Pattern COINS = Pattern.compile("^([\\d,]+) Coins$");
    private static final Pattern ESSENCE = Pattern.compile("^(\\w+) Essence$");
    private static final Pattern BOOK_LORE = Pattern.compile("^(.+) ([IVXLC]+)$");
    private static final Pattern AMOUNT = Pattern.compile("^(.+?) x([\\d,]+)$");

    private static final int GREEN = 0x8055FF55;
    private static final int RED = 0x80FF5555;

    /** The advice for the open chest. {@code rerollSlot} is the index of the "Reroll Chest" button. */
    private record Advice(boolean reroll, double profit, double thresholdCoins, double kismetPrice, int rerollSlot) {
    }

    private @Nullable Advice advice;
    private @Nullable Screen announcedFor;

    @Override
    public Pattern inventoryName() {
        return CHEST_SCREEN;
    }

    @Override
    public boolean isActive() {
        return SkysensConfig.getInstance().dungeonCategory.kismetAdvisor.enabled;
    }

    @Override
    public void afterTick() {
        this.advice = null;
        final KismetAdvisorAccordion config = SkysensConfig.getInstance().dungeonCategory.kismetAdvisor;
        if (!config.enabled) {
            return;
        }
        final Screen screen = Minecraft.getInstance().gui.screen();
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return;
        }
        final Matcher title = CHEST_SCREEN.matcher(screen.getTitle().getString().trim());
        if (!title.matches() || !this.isChestEnabled(title.group(1), config)) {
            return;
        }
        final List<Slot> slots = container.getMenu().slots;
        if (slots.size() <= OPEN_BUTTON_SLOT) {
            return;
        }
        // the Reroll button only exists while a reroll is still possible
        int rerollSlot = -1;
        for (int i = 0; i < slots.size(); i++) {
            if (strip(slots.get(i).getItem().getHoverName().getString()).equals(REROLL_BUTTON)) {
                rerollSlot = i;
                break;
            }
        }
        if (rerollSlot < 0) {
            return;
        }
        final ItemPriceService prices = ItemPriceService.getInstance();
        prices.prefetch();
        prices.prefetchOwn();

        final Double cost = readCost(slots.get(OPEN_BUTTON_SLOT).getItem(), prices);
        if (cost == null) {
            return; // lore not loaded yet
        }
        double value = 0;
        for (int i = FIRST_REWARD_SLOT; i <= LAST_REWARD_SLOT && i < slots.size(); i++) {
            value += value(slots.get(i).getItem(), prices);
        }
        final double profit = value - cost;
        final double threshold = config.thresholdMillions * 1_000_000.0;
        final OptionalDouble kismet = prices.getPriceById(KISMET_ID);
        final double kismetPrice = kismet.isPresent() ? kismet.getAsDouble() : prices.getPrice("Kismet Feather").orElse(-1);
        this.advice = new Advice(profit < threshold, profit, threshold, kismetPrice, rerollSlot);

        if (config.chatMessage && this.announcedFor != screen) {
            this.announcedFor = screen;
            ClientUtil.sendPrefixed("§7Kismet: " + text(this.advice));
        }
    }

    private boolean isChestEnabled(String chest, KismetAdvisorAccordion config) {
        final String type = chest.toUpperCase(Locale.ROOT);
        if (config.onlyBedrock) {
            return type.equals("BEDROCK");
        }
        return type.equals("BEDROCK") || (type.equals("OBSIDIAN") ? config.obsidian : config.otherChests);
    }

    @Override
    public void draw$Head(GuiGraphicsExtractor context, Slot slot, CallbackInfo ci) {
        final Advice current = this.advice;
        final KismetAdvisorAccordion config = SkysensConfig.getInstance().dungeonCategory.kismetAdvisor;
        if (current == null || !(Minecraft.getInstance().gui.screen() instanceof AbstractContainerScreen<?> container)
                || container.getMenu().slots.indexOf(slot) != current.rerollSlot()) {
            return;
        }
        if (config.highlightButton) {
            context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, current.reroll() ? GREEN : RED);
        }
        if (config.showText) {
            // slot coordinates are relative to the chest, so negative y is the strip above it
            context.text(Minecraft.getInstance().font, Component.literal(text(current)), 0, -11, 0xFFFFFFFF, true);
        }
    }

    private static String text(Advice advice) {
        final String kismet = advice.kismetPrice() >= 0 ? ", Kismet " + NumberUtil.formatCoins(advice.kismetPrice()) : "";
        return advice.reroll()
                ? "§aReroll: worth it §7(chest " + NumberUtil.formatCoins(advice.profit()) + " < "
                + NumberUtil.formatCoins(advice.thresholdCoins()) + kismet + ")"
                : "§cKeep: " + NumberUtil.formatCoins(advice.profit())
                + " §7(limit " + NumberUtil.formatCoins(advice.thresholdCoins()) + kismet + ")";
    }

    // --- pricing ---

    /** Coins and key price from the lore of the open button, or null if it has no cost (yet). */
    private static @Nullable Double readCost(ItemStack button, ItemPriceService prices) {
        final ItemLore lore = button.get(DataComponents.LORE);
        if (lore == null) {
            return null;
        }
        boolean found = false;
        double cost = 0;
        for (final Component line : lore.lines()) {
            final String text = strip(line.getString());
            final Matcher coins = COINS.matcher(text);
            if (coins.matches()) {
                cost += parseNumber(coins.group(1));
                found = true;
            } else if (text.equals("FREE")) {
                found = true;
            } else if (text.endsWith(" Key") && !text.startsWith("Requires")) {
                cost += prices.getPrice(text).orElse(0);
                found = true;
            }
        }
        return found ? cost : null;
    }

    /** Value of one reward item in the chest; 0 if empty or unknown. */
    private static double value(ItemStack stack, ItemPriceService prices) {
        if (stack.isEmpty()) {
            return 0;
        }
        String name = strip(stack.getHoverName().getString());
        long amount = stack.getCount();
        final Matcher times = AMOUNT.matcher(name);
        if (times.matches()) {
            name = times.group(1);
            amount = Math.max(amount, (long) parseNumber(times.group(2)));
        }
        final Matcher coins = COINS.matcher(name);
        if (coins.matches()) {
            return parseNumber(coins.group(1));
        }
        final Matcher essence = ESSENCE.matcher(name);
        if (essence.matches()) {
            return prices.getPriceById("ESSENCE_" + essence.group(1).toUpperCase(Locale.ROOT)).orElse(0) * amount;
        }
        if (name.equals("Enchanted Book")) {
            // the enchant is the first line of the lore: "Wisdom II"
            final ItemLore lore = stack.get(DataComponents.LORE);
            if (lore != null && !lore.lines().isEmpty()) {
                final Matcher book = BOOK_LORE.matcher(strip(lore.lines().getFirst().getString()));
                if (book.matches()) {
                    return prices.getBookPrice(book.group(1), ItemIdUtil.romanToInt(book.group(2))).orElse(0);
                }
            }
            return 0;
        }
        return prices.getPrice(name).orElse(0) * amount;
    }

    private static String strip(String text) {
        final String stripped = ChatFormatting.stripFormatting(text);
        return (stripped == null ? text : stripped).trim();
    }

    private static double parseNumber(String number) {
        return Double.parseDouble(number.replace(",", ""));
    }

}
