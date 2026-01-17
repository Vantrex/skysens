package de.vantrex.skysens.client.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.Optional;
import java.util.OptionalInt;

public final class HypixelExtraAttributes {

    private HypixelExtraAttributes() {
    }

    public static Optional<NbtCompound> getExtraAttributes(ItemStack stack) {
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null) return Optional.empty();

        NbtCompound root = custom.copyNbt();

        if (looksLikeExtraAttributes(root)) {
            return Optional.of(root);
        }

        if (root.contains("ExtraAttributes")) {
            return root.getCompound("ExtraAttributes");
        }

        return Optional.empty();
    }
    private static boolean looksLikeExtraAttributes(NbtCompound nbt) {
        return nbt.contains("id")
                || nbt.contains("baseStatBoostPercentage")
                || nbt.contains("item_tier")
                || nbt.contains("uuid");
    }

    public static Optional<NbtCompound> findCompoundByKey(NbtElement element, String key) {
        if (element == null) return Optional.empty();

        if (element instanceof NbtCompound c) {
            if (c.contains(key)) {
                return c.getCompound(key);
            }
            for (String k : c.getKeys()) {
                Optional<NbtCompound> found = findCompoundByKey(c.get(k), key);
                if (found.isPresent()) return found;
            }
        } else if (element instanceof NbtList list) {
            for (int i = 0; i < list.size(); i++) {
                Optional<NbtCompound> found = findCompoundByKey(list.get(i), key);
                if (found.isPresent()) return found;
            }
        }

        return Optional.empty();
    }
    public static Optional<Integer> getInt(NbtCompound ea, String key) {
        if (ea != null && ea.contains(key)) {
            return ea.getInt(key);
        }
        return Optional.empty();
    }

    public static Optional<String> getString(NbtCompound ea, String key) {
        if (ea != null && ea.contains(key)) {
            return ea.getString(key);
        }
        return Optional.empty();
    }
}
