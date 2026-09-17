package de.vantrex.skysens.client.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.ListTag;

import java.util.Optional;

public final class HypixelExtraAttributes {

    private HypixelExtraAttributes() {
    }

    public static Optional<CompoundTag> getExtraAttributes(ItemStack stack) {
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        if (custom == null) return Optional.empty();

        CompoundTag root = custom.copyTag();

        if (looksLikeExtraAttributes(root)) {
            return Optional.of(root);
        }

        if (root.contains("ExtraAttributes")) {
            return root.getCompound("ExtraAttributes");
        }

        return Optional.empty();
    }
    private static boolean looksLikeExtraAttributes(CompoundTag nbt) {
        return nbt.contains("id")
                || nbt.contains("baseStatBoostPercentage")
                || nbt.contains("item_tier")
                || nbt.contains("uuid");
    }

    public static Optional<CompoundTag> findCompoundByKey(Tag element, String key) {
        if (element == null) return Optional.empty();

        if (element instanceof CompoundTag c) {
            if (c.contains(key)) {
                return c.getCompound(key);
            }
            for (String k : c.keySet()) {
                Optional<CompoundTag> found = findCompoundByKey(c.get(k), key);
                if (found.isPresent()) return found;
            }
        } else if (element instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                Optional<CompoundTag> found = findCompoundByKey(list.get(i), key);
                if (found.isPresent()) return found;
            }
        }

        return Optional.empty();
    }
    public static Optional<Integer> getInt(CompoundTag ea, String key) {
        if (ea != null && ea.contains(key)) {
            return ea.getInt(key);
        }
        return Optional.empty();
    }

    public static Optional<String> getString(CompoundTag ea, String key) {
        if (ea != null && ea.contains(key)) {
            return ea.getString(key);
        }
        return Optional.empty();
    }
}
