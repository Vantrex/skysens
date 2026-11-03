package de.vantrex.skysens.client.integration.inventoryhud;

import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

/**
 * Reads where Inventory HUD+ (mod id {@code inventoryhud}) draws its inventory HUD, via reflection on the public static
 * fields of {@code InventoryHUDCommon} and {@code InventoryGui}. The geometry mirrors
 * {@code InventoryGui.renderInventory} (version 3.4.x), including the background texture that reaches 8 pixels past the
 * slots. Any reflective failure logs one warning and switches the bridge off for the session.
 */
public final class InventoryHudBridge {

    private static final Logger LOGGER = LoggerFactory.getLogger("skysens");

    private static final InventoryHudBridge instance = new InventoryHudBridge();

    private final boolean loaded = FabricLoader.getInstance().isModLoaded("inventoryhud");
    private boolean resolved;
    private boolean broken;

    private Field isActive;
    private Field invX;
    private Field invY;
    private Field invAligns;
    private Field invMini;
    private Field invVert;
    private Field horAlign;
    private Field vertAlign;

    private InventoryHudBridge() {
    }

    public static InventoryHudBridge getInstance() {
        return instance;
    }

    public boolean isAvailable() {
        return this.loaded && !this.broken;
    }

    /** False while Inventory HUD+ is loaded but hasn't created its HUD (and read its position) yet. */
    public boolean isReady() {
        if (!this.isAvailable() || !this.resolve()) {
            return false;
        }
        try {
            return this.invAligns.get(null) != null;
        } catch (IllegalAccessException | IllegalArgumentException e) {
            this.disable(e);
            return false;
        }
    }

    /** The area the inventory HUD covers, in GUI-scaled pixels, or null if it is toggled off or not ready. */
    public @Nullable Bounds bounds(int guiWidth, int guiHeight) {
        if (!this.isAvailable() || !this.resolve()) {
            return null;
        }
        try {
            final Object aligns = this.invAligns.get(null);
            if (aligns == null || !this.isActive.getBoolean(null)) {
                return null;
            }
            if (!(this.horAlign.get(aligns) instanceof Enum<?> h) || !(this.vertAlign.get(aligns) instanceof Enum<?> v)) {
                return null;
            }

            final boolean vertical = this.invVert.getBoolean(null);
            final float scale = this.invMini.getBoolean(null) ? 0.75F : 1.0F;
            final int columns = vertical ? 3 : 9;
            final int rows = vertical ? 9 : 3;

            final int x = position(guiWidth, (int) ((columns * 18 - 2) * scale), this.invX.getInt(null), h.name());
            final int y = position(guiHeight, (int) ((rows * 18 - 2) * scale), this.invY.getInt(null), v.name());

            // The background is drawn at (-8, -8) with size (n * 18 + 14), scaled like the slots
            final int left = (int) Math.floor(x - 8 * scale);
            final int top = (int) Math.floor(y - 8 * scale);
            final int width = (int) Math.ceil((columns * 18 + 14) * scale);
            final int height = (int) Math.ceil((rows * 18 + 14) * scale);
            return new Bounds(left, top, width, height);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            this.disable(e);
            return null;
        }
    }

    /** Same as {@code InventoryGui.getX/getY}: start/centre/end anchored, clamped onto the screen. */
    private static int position(int screen, int size, int offset, String align) {
        final int pos = switch (align) {
            case "LEFT", "TOP" -> offset;
            case "MIDDLE", "CENTER" -> screen / 2 - size / 2 - offset;
            default -> screen - offset;
        };
        return Math.clamp(pos, 0, Math.max(0, screen - size));
    }

    private boolean resolve() {
        if (this.resolved) {
            return true;
        }
        try {
            final Class<?> common = Class.forName("dlovin.inventoryhud.InventoryHUDCommon");
            final Class<?> gui = Class.forName("dlovin.inventoryhud.gui.InventoryGui");
            final Class<?> aligns = Class.forName("dlovin.inventoryhud.utils.WidgetAligns");
            this.isActive = common.getField("isActive");
            this.invX = gui.getField("invX");
            this.invY = gui.getField("invY");
            this.invAligns = gui.getField("InvAligns");
            this.invMini = gui.getField("invMini");
            this.invVert = gui.getField("invVert");
            this.horAlign = aligns.getField("HorAlign");
            this.vertAlign = aligns.getField("VertAlign");
            this.resolved = true;
            return true;
        } catch (ReflectiveOperationException | LinkageError e) {
            this.disable(e);
            return false;
        }
    }

    private void disable(Throwable cause) {
        this.broken = true;
        LOGGER.warn("Inventory HUD+ integration disabled, its internals changed: {}", cause.toString());
    }

    /** A screen rectangle in GUI-scaled pixels. */
    public record Bounds(int x, int y, int width, int height) {

        public int right() {
            return this.x + this.width;
        }

        public int bottom() {
            return this.y + this.height;
        }

        public boolean intersects(int otherX, int otherY, int otherWidth, int otherHeight) {
            return this.x < otherX + otherWidth && otherX < this.right()
                    && this.y < otherY + otherHeight && otherY < this.bottom();
        }
    }

}
