package de.vantrex.skysens.client.notification;

import org.jetbrains.annotations.NotNull;
import net.minecraft.sounds.SoundEvent;

/** A sound played when its notification is displayed. */
public record NotificationSound(@NotNull SoundEvent sound, float volume, float pitch) {
}
