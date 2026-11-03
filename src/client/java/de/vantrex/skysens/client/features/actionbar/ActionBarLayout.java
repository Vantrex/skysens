package de.vantrex.skysens.client.features.actionbar;

import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.integration.inventoryhud.InventoryHudBridge;
import lombok.Getter;

/**
 * Moves the vanilla action bar and the {@link de.vantrex.skysens.client.notification.SecondaryActionBar} up above
 * Inventory HUD+'s inventory HUD when the two overlap. If they don't overlap nothing moves.
 * <p>
 * The overlap check doesn't run every frame. It runs once after something marks the layout dirty: Inventory HUD+'s
 * key bindings (show/hide toggle), closing one of its config screens (position changes) or a change of the GUI-scaled
 * window size. Client thread.
 */
public final class ActionBarLayout {

    /** Vanilla translates the action bar to 68 pixels above the bottom and draws the text from 4 pixels above that. */
    private static final int VANILLA_TOP = 68 + 4;
    /** The secondary action bar's text starts 84 pixels above the bottom, one line above the vanilla one. */
    private static final int SECONDARY_TOP = 68 + 16;
    /** Vanilla text is 9 pixels high and its backdrop reaches 2 more pixels down. */
    private static final int VANILLA_BOTTOM = VANILLA_TOP - 9 - 2;
    /**
     * Horizontal room the action bars are assumed to take, centred on the screen. The real width depends on the
     * message, Hypixel's health/defense/mana bar is about this wide. A fixed width keeps the check out of the frame loop.
     */
    private static final int ASSUMED_WIDTH = 260;
    /** Free pixels between the moved action bars and the top of the inventory HUD. */
    private static final int GAP = 2;

    @Getter
    private static final ActionBarLayout instance = new ActionBarLayout();

    private boolean dirty = true;
    private int guiWidth = -1;
    private int guiHeight = -1;
    private int lift;

    private ActionBarLayout() {
    }

    /** Recompute the offset on the next frame, e.g. because the inventory HUD was shown, hidden or moved. */
    public void markDirty() {
        this.dirty = true;
    }

    /**
     * How many pixels the action bars are moved up. Cheap: only recomputes after {@link #markDirty()} or when the
     * GUI-scaled window size changed.
     */
    public int lift(int guiWidth, int guiHeight) {
        if (!SkysensConfig.getInstance().qolCategory.moveActionBarAboveInventoryHud) {
            return 0;
        }
        final InventoryHudBridge bridge = InventoryHudBridge.getInstance();
        if (!bridge.isAvailable()) {
            return 0;
        }
        if (this.dirty || guiWidth != this.guiWidth || guiHeight != this.guiHeight) {
            this.recompute(bridge, guiWidth, guiHeight);
        }
        return this.lift;
    }

    private void recompute(InventoryHudBridge bridge, int guiWidth, int guiHeight) {
        this.lift = 0;
        if (!bridge.isReady()) {
            // stays dirty, Inventory HUD+ hasn't created its HUD yet
            return;
        }
        this.dirty = false;
        this.guiWidth = guiWidth;
        this.guiHeight = guiHeight;

        final InventoryHudBridge.Bounds hud = bridge.bounds(guiWidth, guiHeight);
        if (hud == null) {
            return;
        }
        final int top = guiHeight - SECONDARY_TOP;
        final int bottom = guiHeight - VANILLA_BOTTOM;
        final int left = (guiWidth - ASSUMED_WIDTH) / 2;
        if (!hud.intersects(left, top, ASSUMED_WIDTH, bottom - top)) {
            return;
        }
        // Lift both bars so the vanilla one ends GAP pixels above the HUD, but never off the top of the screen
        this.lift = Math.clamp(bottom - (hud.y() - GAP), 0, Math.max(0, top));
    }

}
