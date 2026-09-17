package de.vantrex.skysens.server.controller;

import de.vantrex.skysens.common.api.ManifestApi;
import de.vantrex.skysens.common.dto.manifest.ModManifestDto;
import de.vantrex.skysens.server.service.ManifestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * §5.7 — the anonymous surface. Must never require a token: the mod calls this
 * before it has one, and a 401 here would break startup for every user.
 */
@RestController
@RequestMapping("/api/v1/manifest")
@RequiredArgsConstructor
public class ManifestController implements ManifestApi {

    private final ManifestService manifestService;

    @Override
    @GetMapping
    public ModManifestDto getManifest() {
        // TODO(deferred): return manifestService.getManifest();
        throw new UnsupportedOperationException("TODO(deferred): manifest retrieval");
    }
}
