package br.com.casellisoftware.budgetmanager.configs.flag;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FlagsProperties.class)
public class FlagBeanConfiguration {

    @Bean
    public FlagManager flagManager(FlagsProperties properties) {
        return new FlagManagerImpl(properties);
    }
}
