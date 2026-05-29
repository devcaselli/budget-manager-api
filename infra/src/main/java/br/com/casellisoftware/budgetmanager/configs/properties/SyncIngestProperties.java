package br.com.casellisoftware.budgetmanager.configs.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the ingest-sync feature.
 *
 * <p>Bound from {@code app.sync.ingest.*} in application YAML.</p>
 */
@ConfigurationProperties(prefix = "app.sync.ingest")
public record SyncIngestProperties(
        String baseUrl,
        String apiKey,
        Cron cron,
        int activeOwnerWindowDays,
        int pageSize
) {
    public record Cron(boolean enabled, String expression) {
    }

    public SyncIngestProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("app.sync.ingest.base-url must not be blank");
        }
        if (pageSize <= 0) {
            pageSize = 100;
        }
        if (cron == null) {
            cron = new Cron(true, "0 */15 22-23 * * *");
        }
    }
}
