package de.vantrex.skysens.client.model;

import de.vantrex.skysens.client.enums.NotificationDisplayTypeEnum;
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

    @Override
    public int compareTo(@NotNull Notification o) {
        return creationTime.compareTo(o.creationTime);
    }

}
