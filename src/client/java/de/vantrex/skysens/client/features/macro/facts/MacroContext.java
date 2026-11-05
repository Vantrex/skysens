package de.vantrex.skysens.client.features.macro.facts;

import de.vantrex.skysens.client.features.macro.engine.InputChord;
import de.vantrex.skysens.client.features.macro.engine.PressMode;
import de.vantrex.skysens.client.skyblock.item.HypixelExtraAttributes;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Per-press snapshot of what placeholders and (later) conditions read. Facts are read lazily, once. */
public final class MacroContext {

    private final InputChord chord;
    private final PressMode mode;
    private boolean heldLoaded;
    private ItemStack held = ItemStack.EMPTY;
    private @Nullable CompoundTag heldExtra;

    public MacroContext(InputChord chord, PressMode mode) {
        this.chord = chord;
        this.mode = mode;
    }

    public InputChord chord() {
        return this.chord;
    }

    public PressMode mode() {
        return this.mode;
    }

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

    public ItemStack heldItem() {
        this.loadHeld();
        return this.held;
    }

    /** The held item's SkyBlock ExtraAttributes, read once per press. */
    public Optional<CompoundTag> heldExtraAttributes() {
        this.loadHeld();
        return Optional.ofNullable(this.heldExtra);
    }

}
