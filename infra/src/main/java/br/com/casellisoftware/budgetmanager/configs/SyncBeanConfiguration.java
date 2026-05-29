package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.sync.boundary.GetSyncPreferenceBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.boundary.SyncAllOwnersBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.boundary.SyncIngestBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.boundary.UpdateSyncPreferenceBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.usecase.EnsureSyncPlaceholderCardUseCase;
import br.com.casellisoftware.budgetmanager.application.sync.usecase.GetSyncPreferenceUseCase;
import br.com.casellisoftware.budgetmanager.application.sync.usecase.ResolveCreditCardForIngestUseCase;
import br.com.casellisoftware.budgetmanager.application.sync.usecase.ResolveIngestWalletUseCase;
import br.com.casellisoftware.budgetmanager.application.sync.usecase.SyncIngestForAllOwnersUseCase;
import br.com.casellisoftware.budgetmanager.application.sync.usecase.SyncIngestForOwnerUseCase;
import br.com.casellisoftware.budgetmanager.application.sync.usecase.UpdateSyncPreferenceUseCase;
import br.com.casellisoftware.budgetmanager.configs.properties.SyncIngestProperties;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.sync.IngestPendingSource;
import br.com.casellisoftware.budgetmanager.domain.sync.SyncPreferenceRepository;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.sync.HttpIngestPendingSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(SyncIngestProperties.class)
public class SyncBeanConfiguration {

    @Bean
    public IngestPendingSource ingestPendingSource(SyncIngestProperties properties) {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("X-API-Key", properties.apiKey() != null ? properties.apiKey() : "")
                .build();
        return new HttpIngestPendingSource(restClient);
    }

    @Bean
    public EnsureSyncPlaceholderCardUseCase ensureSyncPlaceholderCardUseCase(CreditCardRepository creditCardRepository) {
        return new EnsureSyncPlaceholderCardUseCase(creditCardRepository);
    }

    @Bean
    public ResolveCreditCardForIngestUseCase resolveCreditCardForIngestUseCase(
            CreditCardRepository creditCardRepository,
            EnsureSyncPlaceholderCardUseCase ensurePlaceholder) {
        return new ResolveCreditCardForIngestUseCase(creditCardRepository, ensurePlaceholder);
    }

    @Bean
    public ResolveIngestWalletUseCase resolveIngestWalletUseCase(WalletRepository walletRepository) {
        return new ResolveIngestWalletUseCase(walletRepository);
    }

    @Bean
    public SyncIngestBoundary syncIngestBoundary(IngestPendingSource ingestPendingSource,
                                                 ExpenseRepository expenseRepository,
                                                 ResolveCreditCardForIngestUseCase resolveCreditCard,
                                                 ResolveIngestWalletUseCase resolveWallet,
                                                 Clock clock) {
        return new SyncIngestForOwnerUseCase(ingestPendingSource, expenseRepository, resolveCreditCard, resolveWallet, clock);
    }

    @Bean
    public SyncAllOwnersBoundary syncAllOwnersBoundary(SyncPreferenceRepository syncPreferenceRepository,
                                                       SyncIngestBoundary syncIngestBoundary) {
        return new SyncIngestForAllOwnersUseCase(syncPreferenceRepository, syncIngestBoundary);
    }

    @Bean
    public GetSyncPreferenceBoundary getSyncPreferenceBoundary(SyncPreferenceRepository syncPreferenceRepository) {
        return new GetSyncPreferenceUseCase(syncPreferenceRepository);
    }

    @Bean
    public UpdateSyncPreferenceBoundary updateSyncPreferenceBoundary(SyncPreferenceRepository syncPreferenceRepository) {
        return new UpdateSyncPreferenceUseCase(syncPreferenceRepository);
    }
}
