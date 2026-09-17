package de.vantrex.skysens.server.service;

import de.vantrex.skysens.common.dto.common.PageDto;
import de.vantrex.skysens.common.dto.notification.NotificationPresetDto;

import java.util.UUID;

public interface NotificationPresetService {

    PageDto<NotificationPresetDto> browse(String featureId, int page, int size);

    NotificationPresetDto publish(UUID authorUuid, NotificationPresetDto preset);

    void delete(UUID authorUuid, UUID presetId);
}
