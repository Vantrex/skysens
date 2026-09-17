package de.vantrex.skysens.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * HTTP-layer wiring: CORS and any message-converter tuning.
 *
 * <p>Note the deliberate asymmetry with the mod: the server speaks Jackson (from
 * the web starter), the mod speaks the Gson that Minecraft already bundles. Only
 * the DTO <em>shapes</em> in {@code skysens-common} are shared, never a codec.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(final CorsRegistry registry) {
        // TODO(deferred): restrict origins once a web frontend exists; the mod
        // itself is not a browser and needs no CORS at all.
    }
}
