package de.vantrex.skysens.common.dto.notification;

import de.vantrex.skysens.common.domain.notification.NotificationDisplayTypeEnum;

import java.time.Instant;
import java.util.UUID;

/**
 * Shareable preset for the {@code mining/notification/} features (§5.6).
 */
public record NotificationPresetDto(
        UUID presetId,
        String name,
        String featureId,
        NotificationDisplayTypeEnum displayType,
        long leadTimeMillis,
        UUID authorUuid,
        String authorName,
        Instant publishedAt
) {
}
