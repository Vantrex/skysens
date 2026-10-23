package de.vantrex.skysens.client.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;

@AllArgsConstructor
@Builder
@Getter
public final class Notification implements Comparable<Notification> {

    private final @NotNull Component text;
    private final @Nullable Component subtitle;
    private @Nullable NotificationDisplayTypeEnum displayType;
    @Builder.Default
    private final Instant creationTime = Instant.now();
    private @Nullable Duration displayTime;
    /** Played once the notification is displayed, not when it is queued. */
    private final @Nullable NotificationSound sound;
    @Builder.Default
    private final @NotNull NotificationLevelEnum level = NotificationLevelEnum.NORMAL;

    /** Important notifications first, then oldest first. */
    @Override
    public int compareTo(@NotNull Notification o) {
        final int byLevel = o.level.compareTo(level);
        return byLevel != 0 ? byLevel : creationTime.compareTo(o.creationTime);
    }

}
