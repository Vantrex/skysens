package de.vantrex.skysens.common.api;

import de.vantrex.skysens.common.dto.common.PageDto;
import de.vantrex.skysens.common.dto.sensitivity.PublishProfileRequest;
import de.vantrex.skysens.common.dto.sensitivity.SensitivityProfileDto;

import java.util.UUID;

/**
 * §5.2 — publish / browse / import sensitivity profiles.
 */
public interface SensitivityProfileApi {

    PageDto<SensitivityProfileDto> browse(String query, int page, int size);

    SensitivityProfileDto get(UUID profileId);

    SensitivityProfileDto publish(PublishProfileRequest request);

    void delete(UUID profileId);
}
