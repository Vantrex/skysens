package de.vantrex.skysens.client.util;

import lombok.experimental.UtilityClass;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@UtilityClass
public class ItemUtil {

    /** The lore lines without formatting codes and surrounding spaces, empty if the item has no lore. */
    public @NotNull List<String> strippedLore(@NotNull ItemStack stack) {
        final ItemLore lore = stack.get(DataComponents.LORE);
        return lore == null ? List.of() : lore.lines().stream().map(line -> ChatUtil.strip(line.getString())).toList();
    }

}
