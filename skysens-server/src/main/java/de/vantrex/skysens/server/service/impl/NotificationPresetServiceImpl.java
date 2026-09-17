package de.vantrex.skysens.server.service.impl;

import de.vantrex.skysens.common.dto.common.PageDto;
import de.vantrex.skysens.common.dto.notification.NotificationPresetDto;
import de.vantrex.skysens.server.service.NotificationPresetService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Stub implementation. Every method body is deliberately unimplemented: this pass
 * produces structure only, and zero features are migrated to the server.
 */
@Service
public class NotificationPresetServiceImpl implements NotificationPresetService {

    @Override
    public PageDto<NotificationPresetDto> browse(final String featureId, final int page, final int size) {
        // TODO(deferred): paged list filtered by feature id
        throw new UnsupportedOperationException("TODO(deferred): preset browse");
    }

    @Override
    public NotificationPresetDto publish(final UUID authorUuid, final NotificationPresetDto preset) {
        // TODO(deferred): persist under the author's UUID
        throw new UnsupportedOperationException("TODO(deferred): preset publish");
    }

    @Override
    public void delete(final UUID authorUuid, final UUID presetId) {
        // TODO(deferred): ownership check, then delete
        throw new UnsupportedOperationException("TODO(deferred): preset delete");
    }
}
