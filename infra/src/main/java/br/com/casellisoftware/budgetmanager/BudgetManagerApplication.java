package br.com.casellisoftware.budgetmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.util.Arrays;

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
        boolean isProd = Arrays.asList(environment.getActiveProfiles()).contains("prod");
        boolean securityEnabled = Boolean.parseBoolean(
                environment.getProperty("app.security.enabled", "true"));

        if (isProd && !securityEnabled) {
            throw new IllegalStateException(
                    "Security MUST be enabled in production. " +
                    "Do not set APP_SECURITY_ENABLED=false on a prod environment.");
        }
    }
}
