package de.vantrex.skysens.common.api;

import de.vantrex.skysens.common.dto.common.PageDto;
import de.vantrex.skysens.common.dto.notification.NotificationPresetDto;

import java.util.UUID;

/**
 * §5.6 — shareable notification presets.
 */
public interface NotificationPresetApi {

    PageDto<NotificationPresetDto> browse(String featureId, int page, int size);

    NotificationPresetDto publish(NotificationPresetDto preset);

    void delete(UUID presetId);
}
