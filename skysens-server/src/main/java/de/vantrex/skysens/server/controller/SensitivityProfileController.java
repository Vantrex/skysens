package de.vantrex.skysens.server.controller;

import de.vantrex.skysens.common.api.SensitivityProfileApi;
import de.vantrex.skysens.common.dto.common.PageDto;
import de.vantrex.skysens.common.dto.sensitivity.PublishProfileRequest;
import de.vantrex.skysens.common.dto.sensitivity.SensitivityProfileDto;
import de.vantrex.skysens.server.service.SensitivityProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * §5.2 — publish / browse / import sensitivity profiles.
 */
@RestController
@RequestMapping("/api/v1/profiles/sensitivity")
@RequiredArgsConstructor
public class SensitivityProfileController implements SensitivityProfileApi {

    private final SensitivityProfileService sensitivityProfileService;

    @Override
    @GetMapping
    public PageDto<SensitivityProfileDto> browse(
            @RequestParam(required = false) final String query,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size) {
        // TODO(deferred): paged search over published profiles
        throw new UnsupportedOperationException("TODO(deferred): profile browse");
    }

    @Override
    @GetMapping("/{profileId}")
    public SensitivityProfileDto get(@PathVariable final UUID profileId) {
        // TODO(deferred): fetch one profile and bump its download counter
        throw new UnsupportedOperationException("TODO(deferred): profile fetch");
    }

    @Override
    @PostMapping
    public SensitivityProfileDto publish(@Valid @RequestBody final PublishProfileRequest request) {
        // TODO(deferred): persist under the caller's UUID; author is taken from the
        //                 token, never from the request body
        throw new UnsupportedOperationException("TODO(deferred): profile publish");
    }

    @Override
    @DeleteMapping("/{profileId}")
    public void delete(@PathVariable final UUID profileId) {
        // TODO(deferred): verify ownership against the caller's UUID, then delete
        throw new UnsupportedOperationException("TODO(deferred): profile delete");
    }
}
