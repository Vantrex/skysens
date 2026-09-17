package de.vantrex.skysens.client.mixin;

import de.vantrex.skysens.client.feature.HandledScreenFeature;
import de.vantrex.skysens.client.service.FeatureService;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractContainerScreen.class)
public abstract class HandledScreenMixin {

    @Unique
    private final FeatureService featureService = FeatureService.getInstance();

    @Final
    @Shadow
    protected AbstractContainerMenu menu;

    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;

    @Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
    private void skysens$drawSlot$Head(GuiGraphicsExtractor context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        Component containerTitle = ((Screen) (Object) this).getTitle();
        for (HandledScreenFeature handledScreenFeature : featureService.getFeatureRegistry().getHandledScreenFeature()) {
            if (handledScreenFeature.isActive() && handledScreenFeature.isInventory(containerTitle)) {
                handledScreenFeature.draw$Head(context, slot, ci);
            }
        }
        /*

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
        */
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void skysens$mouseClicked$Head(MouseButtonEvent click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        Component containerTitle = ((Screen) (Object) this).getTitle();
        for (HandledScreenFeature handledScreenFeature : featureService.getFeatureRegistry().getHandledScreenFeature()) {
            if (handledScreenFeature.isActive() && handledScreenFeature.isInventory(containerTitle)) {
                handledScreenFeature.mouseClicked$Head(click, doubled, menu, leftPos, topPos, cir);
            }
        }
    }

}
