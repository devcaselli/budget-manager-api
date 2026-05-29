package br.com.casellisoftware.budgetmanager.configs.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "true", matchIfMissing = true)
public class MdcAuthenticationFilter extends OncePerRequestFilter {

    public static final String OWNER_ID_KEY = "ownerId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ownerId = resolveOwnerId();
        String previousOwnerId = MDC.get(OWNER_ID_KEY);
        if (ownerId != null) {
            MDC.put(OWNER_ID_KEY, ownerId);
        } else {
            MDC.remove(OWNER_ID_KEY);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (previousOwnerId == null) {
                MDC.remove(OWNER_ID_KEY);
            } else {
                MDC.put(OWNER_ID_KEY, previousOwnerId);
            }
        }
    }

    private static String resolveOwnerId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthenticationToken)
                || !authentication.isAuthenticated()) {
            return null;
        }

        String subject = jwtAuthenticationToken.getToken().getSubject();
        return subject == null || subject.isBlank() ? null : subject;
    }
}
