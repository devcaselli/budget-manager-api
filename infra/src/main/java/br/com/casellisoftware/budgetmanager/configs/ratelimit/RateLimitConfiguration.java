package br.com.casellisoftware.budgetmanager.configs.ratelimit;

import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Wires rate-limit infrastructure beans.
 *
 * <p>{@link AuthRateLimitFilter} is registered as a plain Spring bean — Spring Boot
 * auto-registers {@link org.springframework.web.filter.OncePerRequestFilter} beans
 * into the servlet filter chain automatically. No explicit {@code FilterRegistrationBean}
 * needed.</p>
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfiguration {

    @Bean
    public AuthRateLimitFilter authRateLimitFilter(RateLimitProperties props,
                                                    ObjectMapper objectMapper) {
        return new AuthRateLimitFilter(props, objectMapper);
    }
}
