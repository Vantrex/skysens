package de.vantrex.skysens.common.api;

import de.vantrex.skysens.common.dto.manifest.ModManifestDto;

/**
 * §5.7 — unauthenticated. The only endpoint the mod may call before it has a token.
 */
public interface ManifestApi {

    ModManifestDto getManifest();
}
