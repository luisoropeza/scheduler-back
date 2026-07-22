package com.example.scheduler.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Authenticates service-to-service callers (e.g. the n8n WhatsApp workflow) against
 * {@code /api/integrations/**} using a static API key instead of a per-user JWT.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-API-Key";
    private static final String PATH_PREFIX = "/api/integrations/";

    @Value("${n8n.api-key}")
    private String apiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        if (request.getRequestURI().startsWith(PATH_PREFIX)) {
            String providedKey = request.getHeader(HEADER);
            if (providedKey != null && providedKey.equals(apiKey)) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken("n8n-integration", null,
                                List.of(new SimpleGrantedAuthority("ROLE_INTEGRATION"))));
            }
        }
        chain.doFilter(request, response);
    }
}
