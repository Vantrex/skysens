package de.vantrex.skysens.common.api;

import de.vantrex.skysens.common.dto.splits.PersonalBestDto;
import de.vantrex.skysens.common.dto.splits.RunSubmissionDto;
import de.vantrex.skysens.common.dto.splits.SplitDefinitionSetDto;

import java.util.List;

/**
 * §5.1 — split definitions (public read) plus run history and PBs (authenticated).
 */
public interface SplitsApi {

    SplitDefinitionSetDto getSplitDefinitions();

    void submitRun(RunSubmissionDto run);

    List<PersonalBestDto> getPersonalBests();
}
