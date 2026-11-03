package de.vantrex.skysens.client.ui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.hud.HudRegistry;
import de.vantrex.skysens.client.core.hud.MovableHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Moves and scales every {@link MovableHud} ({@code /skysens hud}, or the button in General): drag with the left mouse
 * button, Ctrl + mouse wheel scales, right click resets. Shows all elements with a preview, also switched off ones and
 * outside SkyBlock; the live HUD isn't drawn meanwhile. Saves when closed.
 */
public class HudEditorScreen extends Screen {

    private static final float SCALE_STEP = 0.05f;
    private static final int OUTLINE = 0xFFAAAAAA;
    private static final int OUTLINE_ACTIVE = 0xFFFFFFFF;
    private static final int OUTLINE_DISABLED = 0xFFAA5555;
    private static final int HOVER_FILL = 0x30FFFFFF;
    private static final int LABEL_COLOR = 0xFFFFFFFF;
    private static final int HINT_COLOR = 0xFFCCCCCC;
    private static final String HINT = "Drag to move · Ctrl + Scroll to scale · Right click to reset · Esc to save";

    private final @Nullable Screen parent;
    private List<MovableHud> huds = List.of();
    private @Nullable MovableHud dragging;
    private double grabX;
    private double grabY;

    public HudEditorScreen(@Nullable Screen parent) {
        super(Component.literal("SkySens HUD Editor"));
        this.parent = parent;
    }

    /** Opens the editor. Queued, so a chat screen that is just being closed doesn't close it again. */
    public static void open() {
        final Minecraft client = Minecraft.getInstance();
        client.schedule(() -> {
            // Don't return to the chat screen the command was typed in
            final Screen parent = client.gui.screen() instanceof ChatScreen ? null : client.gui.screen();
            client.gui.setScreen(new HudEditorScreen(parent));
        });
    }

    @Override
    protected void init() {
        this.huds = HudRegistry.getInstance().all();
        HudRegistry.getInstance().setEditing(true);
    }

    @Override
    public void removed() {
        HudRegistry.getInstance().setEditing(false);
        this.dragging = null;
        SkysensConfig.getConfig().saveToFile();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // no blur, the previews have to look like the real HUD
        graphics.fill(0, 0, this.width, this.height, 0x40000000);
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        final MovableHud hovered = this.dragging != null ? this.dragging : this.hudAt(mouseX, mouseY);
        for (final MovableHud hud : this.huds) {
            this.drawHud(graphics, hud, hud == hovered);
        }
        graphics.centeredText(this.font, HINT, this.width / 2, 4, HINT_COLOR);
    }

    private void drawHud(GuiGraphicsExtractor graphics, MovableHud hud, boolean active) {
        final float scale = hud.scale();
        final float left = hud.left(this.width, this.height);
        final float top = hud.top(this.width, this.height);
        graphics.pose().pushMatrix();
        graphics.pose().translate(left, top);
        graphics.pose().scale(scale, scale);
        hud.renderPreview(graphics);
        graphics.pose().popMatrix();

        final int x0 = (int) Math.floor(left) - 1;
        final int y0 = (int) Math.floor(top) - 1;
        final int x1 = (int) Math.ceil(left + hud.width() * scale) + 1;
        final int y1 = (int) Math.ceil(top + hud.height() * scale) + 1;
        if (active) {
            graphics.fill(x0, y0, x1, y1, HOVER_FILL);
        }
        final int color = !hud.isEnabled() ? OUTLINE_DISABLED : active ? OUTLINE_ACTIVE : OUTLINE;
        graphics.fill(x0, y0, x1, y0 + 1, color);
        graphics.fill(x0, y1 - 1, x1, y1, color);
        graphics.fill(x0, y0, x0 + 1, y1, color);
        graphics.fill(x1 - 1, y0, x1, y1, color);

        if (active) {
            final String label = hud.name() + (hud.isEnabled() ? "" : " (disabled)")
                    + String.format(" · %.0f%%", scale * 100f);
            // above the box, or below it if there is no room
            final int labelY = y0 - this.font.lineHeight - 1 >= 0 ? y0 - this.font.lineHeight - 1 : y1 + 2;
            graphics.text(this.font, Component.literal(label), Math.max(0, x0), labelY, LABEL_COLOR, true);
        }
    }

    /** The topmost element under the point, null if there is none. */
    private @Nullable MovableHud hudAt(double x, double y) {
        for (int i = this.huds.size() - 1; i >= 0; i--) {
            final MovableHud hud = this.huds.get(i);
            final float left = hud.left(this.width, this.height);
            final float top = hud.top(this.width, this.height);
            if (x >= left && x <= left + hud.width() * hud.scale() && y >= top && y <= top + hud.height() * hud.scale()) {
                return hud;
            }
        }
        return null;
    }

    /** Moves the element so its top left corner is at the point, but keeps it on the screen. */
    private void moveClamped(MovableHud hud, double left, double top) {
        final float maxLeft = Math.max(0f, this.width - hud.width() * hud.scale());
        final float maxTop = Math.max(0f, this.height - hud.height() * hud.scale());
        hud.moveTo(Math.clamp((float) left, 0f, maxLeft), Math.clamp((float) top, 0f, maxTop), this.width, this.height);
    }

    @Override
    public boolean mouseClicked(@NotNull MouseButtonEvent event, boolean doubleClick) {
        final MovableHud hud = this.hudAt(event.x(), event.y());
        if (hud == null) {
            return super.mouseClicked(event, doubleClick);
        }
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            this.dragging = hud;
            this.grabX = event.x() - hud.left(this.width, this.height);
            this.grabY = event.y() - hud.top(this.width, this.height);
        } else if (event.button() == InputConstants.MOUSE_BUTTON_RIGHT) {
            hud.reset();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(@NotNull MouseButtonEvent event, double dragX, double dragY) {
        if (this.dragging == null || event.button() != InputConstants.MOUSE_BUTTON_LEFT) {
            return super.mouseDragged(event, dragX, dragY);
        }
        this.moveClamped(this.dragging, event.x() - this.grabX, event.y() - this.grabY);
        return true;
    }

    @Override
    public boolean mouseReleased(@NotNull MouseButtonEvent event) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT) {
            this.dragging = null;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        final MovableHud hud = this.dragging != null ? this.dragging : this.hudAt(mouseX, mouseY);
        if (hud == null || scrollY == 0 || !controlDown()) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        final float oldScale = hud.scale();
        final float stepped = Math.round((oldScale + Math.signum((float) scrollY) * SCALE_STEP) / SCALE_STEP) * SCALE_STEP;
        final float newScale = Math.clamp(stepped, hud.minScale(), hud.maxScale());
        if (newScale == oldScale) {
            return true;
        }
        // scale around the element's centre, so it doesn't jump away from the cursor
        final float centreX = hud.left(this.width, this.height) + hud.width() * oldScale / 2f;
        final float centreY = hud.top(this.width, this.height) + hud.height() * oldScale / 2f;
        hud.setScale(newScale);
        this.moveClamped(hud, centreX - hud.width() * newScale / 2f, centreY - hud.height() * newScale / 2f);
        if (this.dragging == hud) {
            this.grabX = mouseX - hud.left(this.width, this.height);
            this.grabY = mouseY - hud.top(this.width, this.height);
        }
        return true;
    }

    private static boolean controlDown() {
        final Window window = Minecraft.getInstance().getWindow();
        return InputConstants.isKeyDown(window, InputConstants.KEY_LCONTROL)
                || InputConstants.isKeyDown(window, InputConstants.KEY_RCONTROL);
    }

}
