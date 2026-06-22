package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.CreateConnectTokenBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.usecase.CreateConnectTokenUseCase;
import br.com.casellisoftware.budgetmanager.configs.properties.PluggyProperties;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import br.com.casellisoftware.budgetmanager.pluggy.HttpPluggyClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(PluggyProperties.class)
public class PluggyBeanConfiguration {

    @Bean
    public PluggyClient pluggyClient(PluggyProperties properties) {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
        return new HttpPluggyClient(restClient, properties.clientId(), properties.clientSecret());
    }

    @Bean
    public CreateConnectTokenBoundary createConnectTokenBoundary(PluggyClient pluggyClient) {
        return new CreateConnectTokenUseCase(pluggyClient);
    }
}
