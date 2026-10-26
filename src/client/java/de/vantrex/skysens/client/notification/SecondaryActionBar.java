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

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * A second action bar, drawn as its own line right above the vanilla one, so Hypixel's action bar (health, defense,
 * mana) stays visible. Like the vanilla action bar it shows one message at a time; messages sent while another one is
 * shown wait their turn. Send through {@link de.vantrex.skysens.client.util.ClientUtil#sendSecondaryActionBar}.
 * Client thread.
 */
public final class SecondaryActionBar {

    /** Vanilla draws the action bar around 68 pixels above the bottom, this one goes one line above it. */
    private static final int OFFSET = 68 + 16;

    @Getter
    private static final SecondaryActionBar instance = new SecondaryActionBar();

    private final Deque<Pending> waiting = new ArrayDeque<>();
    private @Nullable Component message;
    private long untilMs;

    private SecondaryActionBar() {
    }

    /**
     * Shows the message for the given time. Without override it waits while another message is shown, and its time
     * starts once it is shown. With override it replaces the shown message right away.
     */
    public void show(@NotNull Component message, long durationMs, boolean override) {
        if (!override && this.current() != null) {
            this.waiting.add(new Pending(message, durationMs));
            return;
        }
        this.display(message, durationMs);
    }

    /**
     * Hides the message or drops it from the waiting ones, but only if it is the given one, so a caller doesn't hide
     * someone else's message.
     */
    public void clear(@Nullable Component message) {
        if (message == null) {
            return;
        }
        this.waiting.removeIf(pending -> pending.message() == message);
        if (this.message == message) {
            this.message = null;
        }
    }

    /** The shown message, null if there is none. Moves on to the next waiting message once it expired. */
    public @Nullable Component current() {
        final long now = System.currentTimeMillis();
        if (this.message != null && now >= this.untilMs) {
            this.message = null;
        }
        if (this.message == null && !this.waiting.isEmpty()) {
            final Pending next = this.waiting.poll();
            this.display(next.message(), next.durationMs());
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

    private void display(Component message, long durationMs) {
        this.message = message;
        this.untilMs = System.currentTimeMillis() + durationMs;
    }

    private record Pending(Component message, long durationMs) {
    }

}
