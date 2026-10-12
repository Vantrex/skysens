package de.vantrex.skysens.client.feature.combat;

import de.vantrex.skysens.client.feature.AfterTickFeature;
import de.vantrex.skysens.client.feature.GameMessageListeningFeature;
import lombok.Getter;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;

import java.util.regex.Pattern;

/**
 * Remembers when the player was last in combat, so other features can wait with interruptions (e.g. bazaar alerts).
 * Combat is taking damage, hitting an entity or ability damage ("Your Implosion hit 3 enemies for 1,234 damage.").
 * Hidden chat messages still reach this, the chat filter passes them on to SkySens' listeners.
 */
public class CombatTracker implements GameMessageListeningFeature, AfterTickFeature {

    @Getter
    private static final CombatTracker instance = new CombatTracker();

    private static final Pattern ABILITY_DAMAGE = Pattern.compile("^Your .+ hit [\\d,]+ enem(?:y|ies) for [\\d,.]+ damage\\.");

    private volatile long lastCombatAt;

    private CombatTracker() {
        AttackEntityCallback.EVENT.register((player, level, hand, entity, hitResult) -> {
            if (level.isClientSide()) {
                this.markCombat();
            }
            return InteractionResult.PASS;
        });
    }

    /** Whether there was no combat for at least {@code seconds}. */
    public boolean isOutOfCombatFor(double seconds) {
        return System.currentTimeMillis() - this.lastCombatAt >= (long) (seconds * 1000);
    }

    private void markCombat() {
        this.lastCombatAt = System.currentTimeMillis();
    }

    @Override
    public void afterTick() {
        final LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.hurtTime > 0) {
            this.markCombat();
        }
    }

    @Override
    public void onGameMessage(Component message, boolean overlay) {
        if (overlay) {
            return;
        }
        final String text = ChatFormatting.stripFormatting(message.getString());
        if (text != null && ABILITY_DAMAGE.matcher(text.trim()).find()) {
            this.markCombat();
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

}
