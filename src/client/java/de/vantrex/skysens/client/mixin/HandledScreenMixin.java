package de.vantrex.skysens.client.mixin;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.util.ClientUtil;
import de.vantrex.skysens.client.util.DungeonStatParser;
import de.vantrex.skysens.client.util.HypixelExtraAttributes;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.ScreenHandlerProvider;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin {

    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void skysens$drawSlotHighlight(DrawContext context, Slot slot, CallbackInfo ci) {
        ItemStack stack = slot.getStack();
        if (stack.isEmpty()) {
            return;
        }
        HypixelExtraAttributes.getExtraAttributes(
                stack
        )
                .filter(nbtCompound -> HypixelExtraAttributes.getInt(nbtCompound, "baseStatBoostPercentage")
                        .filter(integer -> integer == 50)
                        .isPresent()
                && HypixelExtraAttributes.getString(nbtCompound, "dungeon_skill_req")
                        .filter(s -> s.equals("CATACOMBS:36"))
                        .isPresent())
                .ifPresent(nbtCompound -> {
                    int x = slot.x;
                    int y = slot.y;
                    context.fill(x, y, x + 16, y + 16, 0x8000FF00);
                });
    }

}
