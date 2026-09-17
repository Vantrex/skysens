package de.vantrex.skysens.server.service;

import de.vantrex.skysens.common.dto.splits.PersonalBestDto;
import de.vantrex.skysens.common.dto.splits.RunSubmissionDto;
import de.vantrex.skysens.common.dto.splits.SplitDefinitionSetDto;

import java.util.List;
import java.util.UUID;

public interface SplitsService {

    SplitDefinitionSetDto getSplitDefinitions();

    void submitRun(UUID playerUuid, RunSubmissionDto run);

    List<PersonalBestDto> getPersonalBests(UUID playerUuid);
}
