package de.vantrex.skysens.server.service;

import de.vantrex.skysens.common.dto.common.PageDto;
import de.vantrex.skysens.common.dto.sensitivity.PublishProfileRequest;
import de.vantrex.skysens.common.dto.sensitivity.SensitivityProfileDto;

import java.util.UUID;

public interface SensitivityProfileService {

    PageDto<SensitivityProfileDto> browse(String query, int page, int size);

    SensitivityProfileDto get(UUID profileId);

    SensitivityProfileDto publish(UUID authorUuid, PublishProfileRequest request);

    void delete(UUID authorUuid, UUID profileId);
}
