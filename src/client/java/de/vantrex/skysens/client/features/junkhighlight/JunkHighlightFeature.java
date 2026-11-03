package de.vantrex.skysens.client.features.junkhighlight;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.features.junkhighlight.config.JunkHighlightSubCategory;
import de.vantrex.skysens.client.features.junkhighlight.config.JunkHighlightSubCategory.GearRarity;
import de.vantrex.skysens.client.core.feature.HandledScreenFeature;
import de.vantrex.skysens.client.core.feature.SkySensFeature;
import de.vantrex.skysens.client.skyblock.item.HypixelExtraAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tints junk items in the player inventory while a sell menu (NPC shop, Trades, Booster Cookie) or the
 * Salvage Items menu is open. Highlight only: nothing is ever clicked or moved.
 */
@SkySensFeature
public class JunkHighlightFeature implements HandledScreenFeature {

    private static final Pattern SELL_TITLE = Pattern.compile("(?i)trades|booster cookie.*|.*shop");
    private static final Pattern SALVAGE_TITLE = Pattern.compile("(?i)salvage items");
    private static final Pattern DUNGEON_POTION = Pattern.compile("Dungeon [IVX]+ Potion");
    private static final Pattern WISP_WATER = Pattern.compile("Wisp's Ice-Flavored Water.*");
    private static final Pattern RARITY = Pattern.compile("\\b(UNCOMMON|COMMON|RARE|EPIC|LEGENDARY|MYTHIC)\\b");
    private static final String SELL_BUTTON = "Sell Item";
    private static final long MENU_CACHE_MS = 250;

    private final JunkHighlightSubCategory config = SkysensConfig.getInstance().qolCategory.junkHighlight;

    private String cachedCustomRaw;
    private Set<String> cachedCustom = Set.of();

    private Object cachedScreen;
    private long cachedAt;
    private boolean cachedEligible;

    @Override
    public Pattern inventoryName() {
        // the real title/contents check happens in isEligibleMenu, which also needs the menu slots
        return Pattern.compile(".*", Pattern.DOTALL);
    }

    @Override
    public boolean isActive() {
        return config.highlightInSellMenus || config.highlightInSalvageMenu;
    }

    @Override
    public void draw$Head(GuiGraphicsExtractor context, Slot slot, CallbackInfo ci) {
        if (!(slot.container instanceof Inventory)) {
            return;
        }
        final ItemStack stack = slot.getItem();
        if (stack.isEmpty() || !isEligibleMenu() || !isJunk(stack)) {
            return;
        }
        context.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, config.highlightColour.getEffectiveColourRGB());
    }

    private boolean isEligibleMenu() {
        final Minecraft mc = Minecraft.getInstance();
        final long now = System.currentTimeMillis();
        if (mc.gui.screen() == cachedScreen && now - cachedAt < MENU_CACHE_MS) {
            return cachedEligible;
        }
        cachedScreen = mc.gui.screen();
        cachedAt = now;
        cachedEligible = computeEligible(mc);
        return cachedEligible;
    }

    private boolean computeEligible(Minecraft mc) {
        if (!(mc.gui.screen() instanceof AbstractContainerScreen<?> screen)) {
            return false;
        }
        final String title = screen.getTitle().getString();
        if (config.highlightInSalvageMenu && SALVAGE_TITLE.matcher(title).matches()) {
            return true;
        }
        if (!config.highlightInSellMenus) {
            return false;
        }
        if (SELL_TITLE.matcher(title).matches()) {
            return true;
        }
        // NPC shops have a "Sell Item" button in their bottom row
        for (final Slot slot : screen.getMenu().slots) {
            if (!(slot.container instanceof Inventory) && slot.getItem().getHoverName().getString().equals(SELL_BUTTON)) {
                return true;
            }
        }
        return false;
    }

    private boolean isJunk(ItemStack stack) {
        final String name = stack.getHoverName().getString();
        if (config.dungeonPotions && DUNGEON_POTION.matcher(name).matches()) {
            return true;
        }
        if (config.wispWater && WISP_WATER.matcher(name).matches()) {
            return true;
        }
        final CompoundTag attributes = HypixelExtraAttributes.getExtraAttributes(stack).orElse(null);
        if (config.dungeonGear && attributes != null && isSalvageableGear(stack, attributes)) {
            return true;
        }
        final Set<String> custom = customItems();
        if (custom.isEmpty()) {
            return false;
        }
        if (custom.contains(name.toLowerCase(Locale.ROOT))) {
            return true;
        }
        return attributes != null
                && HypixelExtraAttributes.getString(attributes, "id")
                .filter(id -> custom.contains(id.toLowerCase(Locale.ROOT))).isPresent();
    }

    /** Dungeon-dropped gear carries baseStatBoostPercentage; stars/recombs are read from the usual attributes. */
    private boolean isSalvageableGear(ItemStack stack, CompoundTag attributes) {
        if (!attributes.contains("baseStatBoostPercentage")) {
            return false;
        }
        if (config.protectUpgradedGear
                && (positive(attributes, "rarity_upgrades")
                || positive(attributes, "dungeon_item_level")
                || positive(attributes, "upgrade_level"))) {
            return false;
        }
        final GearRarity rarity = rarityOf(stack);
        return rarity != null && rarity.ordinal() <= config.maxGearRarity.ordinal();
    }

    private static boolean positive(CompoundTag attributes, String key) {
        return HypixelExtraAttributes.getInt(attributes, key).filter(v -> v > 0).isPresent();
    }

    /** Rarity from the last non-empty lore line, e.g. "UNCOMMON DUNGEON HELMET". */
    private static GearRarity rarityOf(ItemStack stack) {
        final ItemLore lore = stack.get(DataComponents.LORE);
        if (lore == null) {
            return null;
        }
        final List<Component> lines = lore.lines();
        for (int i = lines.size() - 1; i >= 0; i--) {
            final String text = lines.get(i).getString().trim();
            if (text.isEmpty()) {
                continue;
            }
            final Matcher matcher = RARITY.matcher(text);
            if (matcher.find()) {
                return GearRarity.valueOf(matcher.group(1));
            }
            return null;
        }
        return null;
    }

    private Set<String> customItems() {
        final String raw = config.customItems;
        if (raw == null) {
            return Set.of();
        }
        if (!raw.equals(cachedCustomRaw)) {
            final Set<String> parsed = new HashSet<>();
            for (final String part : raw.split(",")) {
                final String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    parsed.add(trimmed.toLowerCase(Locale.ROOT));
                }
            }
            cachedCustom = parsed;
            cachedCustomRaw = raw;
        }
        return cachedCustom;
    }

}
