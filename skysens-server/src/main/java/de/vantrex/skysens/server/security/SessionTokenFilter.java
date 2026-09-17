package de.vantrex.skysens.server.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Resolves the {@code Authorization: Bearer <token>} header into a
 * {@link SkysensPrincipal}.
 *
 * <p>Deliberately fails <em>open</em> for the anonymous surface (the version
 * manifest and the public assets) and closed for everything else. The mod must be
 * able to complete startup with no credentials at all.
 */
@Component
public class SessionTokenFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(final HttpServletRequest request,
                                    final HttpServletResponse response,
                                    final FilterChain filterChain)
            throws ServletException, IOException {
        // TODO(deferred): parse the bearer token, validate signature and expiry,
        //                 and publish a SkysensPrincipal into the security context.
        filterChain.doFilter(request, response);
    }
}
