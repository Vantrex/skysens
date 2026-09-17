package de.vantrex.skysens.common.dto.manifest;

import java.util.Map;

/**
 * Unauthenticated startup manifest (§5.7). The mod fetches this once per launch;
 * it is also where API version enforcement is negotiated.
 *
 * @param latestModVersion     newest published mod version, for an update nudge
 * @param minimumModVersion    below this the server refuses authenticated calls
 * @param currentApiVersion    the API version the server speaks
 * @param minimumApiVersion    oldest API version the server still accepts
 * @param featureFlags         remote kill-switches, keyed by feature id; a feature
 *                             absent from the map is NOT disabled — absence means
 *                             "no remote opinion", so the local default wins
 * @param messageOfTheDay      optional operator notice, may be null
 */
public record ModManifestDto(
        String latestModVersion,
        String minimumModVersion,
        int currentApiVersion,
        int minimumApiVersion,
        Map<String, Boolean> featureFlags,
        String messageOfTheDay
) {
}
