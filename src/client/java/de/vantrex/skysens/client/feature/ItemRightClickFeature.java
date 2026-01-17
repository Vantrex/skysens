package de.vantrex.skysens.client.feature;

import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public interface ItemRightClickFeature extends Feature {

    void onItemRightClick(final @NotNull ItemStack itemStack);

}
