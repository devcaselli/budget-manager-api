package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.CreateConnectTokenBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.FindPluggyConnectionsBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.FindPluggyTransactionsBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.GetPluggyItemStatusBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.MaterializePluggyTransactionsBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.RegisterPluggyItemBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.usecase.CreateConnectTokenUseCase;
import br.com.casellisoftware.budgetmanager.application.pluggy.usecase.FindPluggyConnectionsUseCase;
import br.com.casellisoftware.budgetmanager.application.pluggy.usecase.FindPluggyTransactionsUseCase;
import br.com.casellisoftware.budgetmanager.application.pluggy.usecase.GetPluggyItemStatusUseCase;
import br.com.casellisoftware.budgetmanager.application.pluggy.usecase.MaterializePluggyTransactionsUseCase;
import br.com.casellisoftware.budgetmanager.application.pluggy.usecase.RegisterPluggyItemUseCase;
import br.com.casellisoftware.budgetmanager.application.sync.usecase.ResolveCreditCardForIngestUseCase;
import br.com.casellisoftware.budgetmanager.application.sync.usecase.ResolveIngestWalletUseCase;
import br.com.casellisoftware.budgetmanager.configs.properties.PluggyProperties;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionRepository;
import br.com.casellisoftware.budgetmanager.pluggy.HttpPluggyClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

/**
 * Wires the Pluggy bank-sync use cases. {@link ResolveIngestWalletUseCase} and
 * {@link ResolveCreditCardForIngestUseCase} are injected here as the very same beans
 * defined by {@link SyncBeanConfiguration} — Spring resolves them by type, so there is
 * no duplicate resolver instance and no divergence from the ingest-sync behavior.
 */
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
    public CreateConnectTokenBoundary createConnectTokenBoundary(PluggyClient pluggyClient,
                                                                   PluggyConnectionRepository pluggyConnectionRepository) {
        return new CreateConnectTokenUseCase(pluggyClient, pluggyConnectionRepository);
    }

    @Bean
    public GetPluggyItemStatusBoundary getPluggyItemStatusBoundary(PluggyClient pluggyClient,
                                                                     PluggyConnectionRepository pluggyConnectionRepository) {
        return new GetPluggyItemStatusUseCase(pluggyClient, pluggyConnectionRepository);
    }

    @Bean
    public RegisterPluggyItemBoundary registerPluggyItemBoundary(PluggyClient pluggyClient,
                                                                  PluggyConnectionRepository pluggyConnectionRepository,
                                                                  Clock clock) {
        return new RegisterPluggyItemUseCase(pluggyClient, pluggyConnectionRepository, clock);
    }

    @Bean
    public FindPluggyConnectionsBoundary findPluggyConnectionsBoundary(PluggyConnectionRepository pluggyConnectionRepository) {
        return new FindPluggyConnectionsUseCase(pluggyConnectionRepository);
    }

    @Bean
    public FindPluggyTransactionsBoundary findPluggyTransactionsBoundary(PluggyClient pluggyClient,
                                                                          PluggyConnectionRepository pluggyConnectionRepository,
                                                                          ExpenseRepository expenseRepository,
                                                                          Clock clock) {
        return new FindPluggyTransactionsUseCase(pluggyClient, pluggyConnectionRepository, expenseRepository, clock);
    }

    @Bean
    public MaterializePluggyTransactionsBoundary materializePluggyTransactionsBoundary(
            PluggyClient pluggyClient,
            PluggyConnectionRepository pluggyConnectionRepository,
            ExpenseRepository expenseRepository,
            ResolveCreditCardForIngestUseCase resolveCreditCard,
            ResolveIngestWalletUseCase resolveWallet,
            Clock clock) {
        return new MaterializePluggyTransactionsUseCase(
                pluggyClient, pluggyConnectionRepository, expenseRepository, resolveCreditCard, resolveWallet, clock);
    }
}
