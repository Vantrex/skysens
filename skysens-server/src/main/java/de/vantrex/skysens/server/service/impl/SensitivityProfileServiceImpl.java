package de.vantrex.skysens.server.service.impl;

import de.vantrex.skysens.common.dto.common.PageDto;
import de.vantrex.skysens.common.dto.sensitivity.PublishProfileRequest;
import de.vantrex.skysens.common.dto.sensitivity.SensitivityProfileDto;
import de.vantrex.skysens.server.service.SensitivityProfileService;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Stub implementation. Every method body is deliberately unimplemented: this pass
 * produces structure only, and zero features are migrated to the server.
 */
@Service
public class SensitivityProfileServiceImpl implements SensitivityProfileService {

    @Override
    public PageDto<SensitivityProfileDto> browse(final String query, final int page, final int size) {
        // TODO(deferred): paged search, newest or most-downloaded first
        throw new UnsupportedOperationException("TODO(deferred): profile browse");
    }

    @Override
    public SensitivityProfileDto get(final UUID profileId) {
        // TODO(deferred): fetch and increment the download counter
        throw new UnsupportedOperationException("TODO(deferred): profile fetch");
    }

    @Override
    public SensitivityProfileDto publish(final UUID authorUuid, final PublishProfileRequest request) {
        // TODO(deferred): persist; enforce a per-author publish quota (§6 rate limiting)
        throw new UnsupportedOperationException("TODO(deferred): profile publish");
    }

    @Override
    public void delete(final UUID authorUuid, final UUID profileId) {
        // TODO(deferred): ownership check, then delete
        throw new UnsupportedOperationException("TODO(deferred): profile delete");
    }
}
