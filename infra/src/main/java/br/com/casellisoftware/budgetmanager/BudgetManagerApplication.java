package br.com.casellisoftware.budgetmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.util.List;

@SpringBootApplication
public class BudgetManagerApplication {

    private final Environment environment;

    public BudgetManagerApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        SpringApplication.run(BudgetManagerApplication.class, args);
    }

    /**
     * Fail-fast guard: security MUST be enabled in production.
     *
     * <p>Prevents accidental exposure via {@code APP_SECURITY_ENABLED=false}
     * on a prod environment. The app will refuse to start rather than silently
     * serve all requests without authentication.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    void assertSecurityEnabledInProd() {
        boolean isProd = isProdProfile();
        boolean securityEnabled = Boolean.parseBoolean(
                environment.getProperty("app.security.enabled", "true"));

        if (isProd && !securityEnabled) {
            throw new IllegalStateException(
                    "Security MUST be enabled in production. " +
                    "Do not set APP_SECURITY_ENABLED=false on a prod environment.");
        }
    }

    /**
     * Fail-fast guard: MongoDB auto-index creation MUST be disabled in production.
     *
     * <p>{@code spring.data.mongodb.auto-index-creation=true} is a dev convenience that
     * blocks on large collections and must never reach prod. This guard ensures a
     * misconfigured {@code prod.env} or inherited property file is caught at startup
     * rather than discovered under load.</p>
     *
     * <p>Reading directly from {@link Environment} avoids a dependency on
     * {@code DataMongoProperties} which may not be present in test slice contexts.</p>
     *
     * <p>See: {@code application-dev.yaml} where this is intentionally enabled.</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    void assertAutoIndexCreationDisabledInProd() {
        // property name as registered by Spring Data MongoDB auto-configuration
        String raw = environment.getProperty("spring.data.mongodb.auto-index-creation");
        boolean autoIndexEnabled = Boolean.parseBoolean(raw);

        if (isProdProfile() && autoIndexEnabled) {
            throw new IllegalStateException(
                    "spring.data.mongodb.auto-index-creation MUST be false in production. " +
                    "This setting blocks deploys on large collections. " +
                    "Remove or set to false in your prod configuration.");
        }
    }

    private boolean isProdProfile() {
        return List.of(environment.getActiveProfiles()).contains("prod");
    }
}
