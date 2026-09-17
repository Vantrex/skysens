package de.vantrex.skysens.server.service.impl;

import de.vantrex.skysens.common.dto.manifest.ModManifestDto;
import de.vantrex.skysens.server.service.ManifestService;
import org.springframework.stereotype.Service;

/**
 * Stub implementation. Every method body is deliberately unimplemented: this pass
 * produces structure only, and zero features are migrated to the server.
 */
@Service
public class ManifestServiceImpl implements ManifestService {

    @Override
    public ModManifestDto getManifest() {
        // TODO(deferred): assemble from configuration properties + the feature-flag table
        throw new UnsupportedOperationException("TODO(deferred): manifest assembly");
    }
}
