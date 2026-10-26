package de.vantrex.skysens.client.notification;

import de.vantrex.skysens.client.SkysensClient;
import lombok.Getter;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A second action bar, drawn as its own line right above the vanilla one, so Hypixel's action bar (health, defense,
 * mana) stays visible. Like the vanilla action bar it shows one message at a time. Send through
 * {@link de.vantrex.skysens.client.util.ClientUtil#sendSecondaryActionBar}. Client thread.
 */
public final class SecondaryActionBar {

    /** Vanilla draws the action bar around 68 pixels above the bottom, this one goes one line above it. */
    private static final int OFFSET = 68 + 16;

    @Getter
    private static final SecondaryActionBar instance = new SecondaryActionBar();

    private @Nullable Component message;
    private long untilMs;

    private SecondaryActionBar() {
    }

    /**
     * Shows the message for the given time. Without override it is dropped while another message is still shown.
     */
    public void show(@NotNull Component message, long durationMs, boolean override) {
        if (!override && this.current() != null) {
            return;
        }
        this.message = message;
        this.untilMs = System.currentTimeMillis() + durationMs;
    }

    /** Hides the message, but only if it is still the given one, so a caller doesn't hide someone else's message. */
    public void clear(@Nullable Component message) {
        if (message != null && this.message == message) {
            this.message = null;
        }
    }

    /** The shown message, null once it expired. */
    public @Nullable Component current() {
        if (this.message != null && System.currentTimeMillis() >= this.untilMs) {
            this.message = null;
        }
        return this.message;
    }

    public void draw(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        final Component text = this.current();
        if (text == null || !SkysensClient.getInstance().isOnSkyBlock()) {
            return;
        }
        final Minecraft client = Minecraft.getInstance();
        final Font font = client.font;
        final int x = (client.getWindow().getGuiScaledWidth() - font.width(text)) / 2;
        final int y = client.getWindow().getGuiScaledHeight() - OFFSET;
        graphics.text(font, text, x, y, 0xFFFFFFFF, true);
    }

}
