package de.vantrex.skysens.client.features.actionbar;

import de.vantrex.skysens.client.SkysensClient;
import de.vantrex.skysens.client.config.SkysensConfig;
import de.vantrex.skysens.client.core.hud.HudPosition;
import de.vantrex.skysens.client.core.hud.HudRegistry;
import de.vantrex.skysens.client.core.hud.MovableHud;
import de.vantrex.skysens.client.notification.SecondaryActionBar;
import lombok.Getter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Draws the {@link SecondaryActionBar}. Until it is moved in the HUD editor it sits one line above the vanilla action
 * bar, centred, and is lifted together with it by {@link ActionBarLayout}. Once moved, its centre and top are kept in
 * percent of the screen ({@link HudPosition}) and it is no longer lifted. Client thread.
 */
public final class SecondaryActionBarHud implements MovableHud {

    /** Vanilla draws the action bar around 68 pixels above the bottom, this one goes one line above it. */
    private static final int OFFSET = 68 + 16;
    private static final Component SAMPLE = Component.literal("§bSkySens Action Bar");

    @Getter
    private static final SecondaryActionBarHud instance = new SecondaryActionBarHud();

    private SecondaryActionBarHud() {
    }

    private static HudPosition position() {
        return SkysensConfig.getInstance().qolCategory.secondaryActionBarPosition;
    }

    public void draw(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        final Component text = SecondaryActionBar.getInstance().current();
        if (text == null || !SkysensClient.getInstance().isOnSkyBlock() || HudRegistry.getInstance().isEditing()) {
            return;
        }
        final int screenWidth = graphics.guiWidth();
        final int screenHeight = graphics.guiHeight();
        final Font font = Minecraft.getInstance().font;
        final float scale = this.scale();
        graphics.pose().pushMatrix();
        graphics.pose().translate(this.left(screenWidth, font.width(text) * scale), this.top(screenWidth, screenHeight));
        graphics.pose().scale(scale, scale);
        graphics.text(font, text, 0, 0, 0xFFFFFFFF, true);
        graphics.pose().popMatrix();
    }

    /** The text shown in the editor: the current message, or a sample if there is none. */
    private static Component previewText() {
        final Component text = SecondaryActionBar.getInstance().current();
        return text == null ? SAMPLE : text;
    }

    private float left(int screenWidth, float scaledWidth) {
        final float centre = position().moved ? screenWidth * position().x / 100f : screenWidth / 2f;
        return centre - scaledWidth / 2f;
    }

    @Override
    public String name() {
        return "SkySens Action Bar";
    }

    @Override
    public int width() {
        return Minecraft.getInstance().font.width(previewText());
    }

    @Override
    public int height() {
        return Minecraft.getInstance().font.lineHeight;
    }

    @Override
    public float scale() {
        return position().scale;
    }

    @Override
    public void setScale(float scale) {
        position().scale = scale;
    }

    @Override
    public float left(int screenWidth, int screenHeight) {
        return this.left(screenWidth, this.width() * this.scale());
    }

    @Override
    public float top(int screenWidth, int screenHeight) {
        if (position().moved) {
            return screenHeight * position().y / 100f;
        }
        return screenHeight - OFFSET - ActionBarLayout.getInstance().lift(screenWidth, screenHeight);
    }

    @Override
    public void moveTo(float left, float top, int screenWidth, int screenHeight) {
        final HudPosition position = position();
        position.moved = true;
        position.x = Math.clamp((left + this.width() * this.scale() / 2f) * 100f / screenWidth, 0f, 100f);
        position.y = Math.clamp(top * 100f / screenHeight, 0f, 100f);
    }

    @Override
    public void renderPreview(GuiGraphicsExtractor graphics) {
        graphics.text(Minecraft.getInstance().font, previewText(), 0, 0, 0xFFFFFFFF, true);
    }

    @Override
    public void reset() {
        position().reset();
    }

}
