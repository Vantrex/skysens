package de.vantrex.skysens.client.ui.widget;

import de.vantrex.skysens.client.ui.model.OptionNode;
import de.vantrex.skysens.client.ui.render.RenderUtil;
import de.vantrex.skysens.client.ui.render.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Util;

/**
 * Base class of every option widget. Mouse coordinates are always SCREEN coordinates; the screen lays
 * widgets out in screen space each frame (contentTop - scroll), so no translate is needed.
 * Never cache option values; re-read {@code node.get()} each frame.
 */
public abstract class OptionWidget {

    /** How long the jump highlight takes to fade out. */
    private static final float FLASH_MS = 1200f;

    protected final OptionNode node;
    protected int x, y, width;
    protected boolean showBreadcrumb;
    private long flashStart = -1;

    protected OptionWidget(OptionNode node) {
        this.node = node;
    }

    public OptionNode node() {
        return node;
    }

    /** Stores the position/width; subclasses may override to lay out children (call super). */
    public void layout(int x, int y, int width) {
        this.x = x;
        this.y = y;
        this.width = width;
    }

    /** Top y as set by the last {@link #layout} call. */
    public int top() {
        return y;
    }

    /** Current height for the current width (may animate). Valid after {@link #layout}. */
    public abstract int height();

    /** Draws the widget inside its laid-out rectangle. */
    public abstract void render(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick);

    /** Draws popovers; called after the content scissor is removed and after nextStratum(). */
    public void renderOverlay(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
    }

    /** True while a popover of this widget (or a child) is open. */
    public boolean hasOverlay() {
        return false;
    }

    /** Whether the point lies inside the open popover (so clicks there are not "outside"). */
    public boolean overlayContains(double mx, double my) {
        return false;
    }

    /** Closes the popover, if any. */
    public void closeOverlay() {
    }

    /** Called after the option's value was reset from outside the widget; drops stale drag/edit state. */
    public void notifyReset() {
    }

    public boolean mouseClicked(double mx, double my, int button) {
        return false;
    }

    public boolean mouseReleased(double mx, double my, int button) {
        return false;
    }

    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        return false;
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        return false;
    }

    public boolean keyPressed(KeyEvent e) {
        return false;
    }

    public boolean charTyped(CharacterEvent e) {
        return false;
    }

    /** Whether the point is inside [x, x+width) x [y, y+height()). */
    public boolean isHovered(double mx, double my) {
        return mx >= x && mx < x + width && my >= y && my < y + height();
    }

    /** Briefly highlights the widget with a fading accent outline (used when jumping to a search result). */
    public void flash() {
        flashStart = Util.getMillis();
    }

    /** Draws the fading highlight over the top {@code h} pixels of the widget; call last from {@code render}. */
    protected void renderFlash(GuiGraphicsExtractor g, int h) {
        if (flashStart < 0) {
            return;
        }
        float t = (Util.getMillis() - flashStart) / FLASH_MS;
        if (t >= 1f) {
            flashStart = -1;
            return;
        }
        float alpha = (1f - t) * (1f - t);
        int radius = Theme.style().cards() ? Theme.CARD_RADIUS : 0;
        RenderUtil.roundedRect(g, x, y, width, h, radius, RenderUtil.withAlpha(Theme.ACCENT_SOFT, alpha));
        RenderUtil.roundedRing(g, x, y, width, h, radius, RenderUtil.withAlpha(Theme.ACCENT, alpha));
    }

    /** Plays the standard UI click sound. */
    public static void playClick() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    /** Whether the faint "Category › Accordion" line is drawn above the name (used in search results). */
    public void setShowBreadcrumb(boolean showBreadcrumb) {
        this.showBreadcrumb = showBreadcrumb;
    }
}
