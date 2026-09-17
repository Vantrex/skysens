package de.vantrex.skysens.server.controller;

import de.vantrex.skysens.common.api.SplitsApi;
import de.vantrex.skysens.common.dto.splits.PersonalBestDto;
import de.vantrex.skysens.common.dto.splits.RunSubmissionDto;
import de.vantrex.skysens.common.dto.splits.SplitDefinitionSetDto;
import de.vantrex.skysens.server.service.SplitsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * §5.1 — split definitions (public) plus run history and PBs (authenticated).
 */
@RestController
@RequestMapping("/api/v1/splits")
@RequiredArgsConstructor
public class SplitsController implements SplitsApi {

    private final SplitsService splitsService;

    @Override
    @GetMapping("/definitions")
    public SplitDefinitionSetDto getSplitDefinitions() {
        // TODO(deferred): serve the current definition set with an ETag so the mod
        //                 can 304 against its cached copy
        throw new UnsupportedOperationException("TODO(deferred): split definitions");
    }

    @Override
    @PostMapping("/runs")
    public void submitRun(@Valid @RequestBody final RunSubmissionDto run) {
        // TODO(deferred): persist the run against the caller's UUID and recompute PBs
        throw new UnsupportedOperationException("TODO(deferred): run submission");
    }

    @Override
    @GetMapping("/personal-bests")
    public List<PersonalBestDto> getPersonalBests() {
        // TODO(deferred): read PBs for the caller's UUID
        throw new UnsupportedOperationException("TODO(deferred): personal bests");
    }
}
