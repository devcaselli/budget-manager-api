package br.com.casellisoftware.budgetmanager.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class SharedBeanConfiguration {

    @Bean
    // Single Clock bean shared across time-aware use cases to keep date behavior consistent.
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
