package de.vantrex.skysens.client.chatcommand.warp;

import de.vantrex.skysens.client.repository.CoopRepository.Coop;
import de.vantrex.skysens.client.repository.CoopRepository.Member;
import de.vantrex.skysens.client.util.TimeUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Lists the cached co-op members from {@link CoopMemberTracker}, a click sends {@code !warpto <member>} to co-op chat.
 * Without cached members it explains how to get them.
 */
public class WarpToMenuScreen extends Screen {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int BUTTON_GAP = 4;
    private static final int TEXT_COLOR = 0xFFFFFFFF;
    private static final int HINT_COLOR = 0xFFAAAAAA;

    private List<Member> members = List.of();
    /** Shown above the buttons, or in place of them if there are no members. */
    private List<String> hint = List.of();
    private int top;

    public WarpToMenuScreen() {
        super(Component.literal("Warp to co-op member"));
    }

    /** Opens the menu. Queued, so a chat screen that is just being closed doesn't close it again. */
    public static void open() {
        final Minecraft client = Minecraft.getInstance();
        client.schedule(() -> client.gui.setScreen(new WarpToMenuScreen()));
    }

    @Override
    protected void init() {
        final CoopMemberTracker tracker = CoopMemberTracker.getInstance();
        tracker.refresh();
        final Coop coop = tracker.getCoop();
        final boolean live = tracker.isLive();
        this.members = coop == null ? List.of() : coop.members;
        this.hint = hint(coop, live);

        final int hintHeight = this.members.isEmpty() ? this.hint.size() * (this.font.lineHeight + 2) + BUTTON_GAP : 0;
        final int buttonsHeight = (this.members.size() + 1) * (BUTTON_HEIGHT + BUTTON_GAP);
        this.top = Math.max(40, (this.height - hintHeight - buttonsHeight) / 2);
        final int left = (this.width - BUTTON_WIDTH) / 2;
        int y = this.top + hintHeight;
        for (final Member member : this.members) {
            final String label = member.online ? member.name
                    : member.name + " (Offline" + (member.offlineFor == null ? "" : " " + member.offlineFor) + ")";
            final Button button = Button.builder(Component.literal(label), b -> this.warpTo(member.name))
                    .bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
            // a cached offline status may be outdated, the warp just doesn't happen if they are offline
            button.active = member.online || !live;
            this.addRenderableWidget(button);
            y += BUTTON_HEIGHT + BUTTON_GAP;
        }
        this.addRenderableWidget(Button.builder(Component.literal("Cancel"), b -> this.onClose())
                .bounds(left, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    private static List<String> hint(Coop coop, boolean live) {
        if (coop == null) {
            return List.of("No co-op members registered yet.",
                    "Visit your private island once, SkySens reads them from the tab list.");
        }
        if (coop.members.isEmpty()) {
            return List.of("No other co-op members found on this profile.");
        }
        if (live) {
            return List.of();
        }
        final long minutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - coop.lastSeenAt);
        return List.of("Online status from your private island, " + TimeUtil.formatAge(minutes));
    }

    private void warpTo(String member) {
        this.onClose();
        final ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            // our own client sees the message too and arms the invite auto-accept, see WarpToService
            connection.sendCommand("cchat !warpto " + member);
        }
    }

    @Override
    public void extractRenderState(@NotNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, this.title, this.width / 2, this.top - 30, TEXT_COLOR);
        // in place of the member buttons if there are none, else just above them
        int y = this.members.isEmpty() ? this.top : this.top - 16;
        for (final String line : this.hint) {
            graphics.centeredText(this.font, line, this.width / 2, y, HINT_COLOR);
            y += this.font.lineHeight + 2;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

}
