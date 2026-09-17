package de.vantrex.skysens.server.service.impl;

import de.vantrex.skysens.common.dto.splits.PersonalBestDto;
import de.vantrex.skysens.common.dto.splits.RunSubmissionDto;
import de.vantrex.skysens.common.dto.splits.SplitDefinitionSetDto;
import de.vantrex.skysens.server.service.SplitsService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Stub implementation. Every method body is deliberately unimplemented: this pass
 * produces structure only, and zero features are migrated to the server.
 */
@Service
public class SplitsServiceImpl implements SplitsService {

    @Override
    public SplitDefinitionSetDto getSplitDefinitions() {
        // TODO(deferred): load the active definition set and attach its hash
        throw new UnsupportedOperationException("TODO(deferred): split definitions");
    }

    @Override
    public void submitRun(final UUID playerUuid, final RunSubmissionDto run) {
        // TODO(deferred): validate against the referenced split set, persist, update PBs
        throw new UnsupportedOperationException("TODO(deferred): run submission");
    }

    @Override
    public List<PersonalBestDto> getPersonalBests(final UUID playerUuid) {
        // TODO(deferred): project PBs from run history
        throw new UnsupportedOperationException("TODO(deferred): personal bests");
    }
}
