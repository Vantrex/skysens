package de.vantrex.skysens.client.feature.afk;

import de.vantrex.skysens.client.feature.AfterTickFeature;
import lombok.Getter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Tells whether the player is AFK, so notifications can wait until they are back.
 * The position is sampled every minute; if it did not change, the player is AFK until they move away from it.
 * Being moved by the server does not count as being back: a server switch (e.g. kicked to the hub by a server restart)
 * or a teleport only moves the reference position, so an AFK player stays AFK.
 */
public class AfkTracker implements AfterTickFeature {

    @Getter
    private static final AfkTracker instance = new AfkTracker();

    private static final long SAMPLE_INTERVAL_MS = 60_000L;
    /** After a server switch the player may still fall or be placed, that is not movement. */
    private static final long SETTLE_MS = 5_000L;
    /** Squared distance that still counts as the same position (0.1 blocks). */
    private static final double SAME_POSITION_SQR = 0.1 * 0.1;
    /** Squared distance within one tick that is a teleport, not walking (8 blocks). */
    private static final double TELEPORT_SQR = 8 * 8;

    private volatile boolean afk;

    private ClientLevel level;
    private Vec3 reference;
    private Vec3 lastPosition;
    private long nextSampleAt;
    private long settleUntil;

    private AfkTracker() {
    }

    public boolean isAfk() {
        return this.afk;
    }

    @Override
    public void afterTick() {
        final Minecraft client = Minecraft.getInstance();
        final LocalPlayer player = client.player;
        if (player == null || client.level == null) {
            return;
        }
        final long now = System.currentTimeMillis();
        final Vec3 position = player.position();

        if (client.level != this.level) {
            // new server or world, keep the AFK state and wait until the player is placed
            this.level = client.level;
            this.settleUntil = now + SETTLE_MS;
        }
        final boolean teleported = this.lastPosition != null && this.lastPosition.distanceToSqr(position) > TELEPORT_SQR;
        this.lastPosition = position;
        if (now < this.settleUntil || teleported || this.reference == null) {
            this.reference = position;
            this.nextSampleAt = Math.max(now, this.settleUntil) + SAMPLE_INTERVAL_MS;
            return;
        }

        final boolean samePosition = this.reference.distanceToSqr(position) <= SAME_POSITION_SQR;
        if (this.afk) {
            if (!samePosition) {
                this.afk = false;
                this.reference = position;
                this.nextSampleAt = now + SAMPLE_INTERVAL_MS;
            }
            return;
        }
        if (now >= this.nextSampleAt) {
            if (samePosition) {
                this.afk = true;
            } else {
                this.reference = position;
            }
            this.nextSampleAt = now + SAMPLE_INTERVAL_MS;
        }
    }

    @Override
    public boolean isActive() {
        return true;
    }

}
