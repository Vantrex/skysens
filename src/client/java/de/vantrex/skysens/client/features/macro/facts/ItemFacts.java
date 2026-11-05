package de.vantrex.skysens.client.features.macro.facts;

import de.vantrex.skysens.client.skyblock.item.HypixelExtraAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The held item and the inventory by SkyBlock id (ExtraAttributes "id"), never by the vanilla material. A stack without
 * ExtraAttributes has no id. The held item's attributes are read once per press.
 */
public final class ItemFacts {

    private static final int GLOB_CACHE_SIZE = 64;
    private static final Map<String, Pattern> GLOBS = new LinkedHashMap<>(16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, Pattern> eldest) {
            return size() > GLOB_CACHE_SIZE;
        }
    };

    private boolean heldLoaded;
    private ItemStack held = ItemStack.EMPTY;
    private @Nullable CompoundTag heldExtra;

    private void loadHeld() {
        if (this.heldLoaded) {
            return;
        }
        this.heldLoaded = true;
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            this.held = mc.player.getMainHandItem();
            this.heldExtra = HypixelExtraAttributes.getExtraAttributes(this.held).orElse(null);
        }
    }

    public ItemStack held() {
        this.loadHeld();
        return this.held;
    }

    public Optional<CompoundTag> heldExtraAttributes() {
        this.loadHeld();
        return Optional.ofNullable(this.heldExtra);
    }

    /** The held item's SkyBlock id, null for an empty hand or a stack without one. */
    public @Nullable String heldId() {
        return this.heldExtraAttributes().flatMap(ea -> HypixelExtraAttributes.getString(ea, "id")).orElse(null);
    }

    /** Case-insensitive match where {@code *} matches anything: {@code *_SWORD}, {@code HYPERION}. */
    public static synchronized boolean matchesGlob(@Nullable String id, String glob) {
        if (id == null) {
            return false;
        }
        Pattern pattern = GLOBS.get(glob);
        if (pattern == null) {
            final StringBuilder regex = new StringBuilder();
            boolean first = true;
            for (String part : glob.split("\\*", -1)) {
                if (!first) {
                    regex.append(".*");
                }
                first = false;
                if (!part.isEmpty()) {
                    regex.append(Pattern.quote(part));
                }
            }
            pattern = Pattern.compile(regex.toString(), Pattern.CASE_INSENSITIVE);
            GLOBS.put(glob, pattern);
        }
        return pattern.matcher(id).matches();
    }

    /** How many items in the inventory (main inventory, hotbar and offhand) have a SkyBlock id matching the glob. */
    public int countInventory(String glob) {
        final Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0;
        }
        final Inventory inventory = mc.player.getInventory();
        int count = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            if (slot >= 36 && slot != Inventory.SLOT_OFFHAND) {
                continue;
            }
            final ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            final String id = HypixelExtraAttributes.getExtraAttributes(stack)
                    .flatMap(ea -> HypixelExtraAttributes.getString(ea, "id")).orElse(null);
            if (matchesGlob(id, glob)) {
                count += stack.getCount();
            }
        }
        return count;
    }

}
