package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.SaveBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.usecase.SaveBulletUseCase;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.usecase.FindWalletDomainByIdUseCase;
import br.com.casellisoftware.budgetmanager.configs.TransactionConfiguration;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletRepository;
import br.com.casellisoftware.budgetmanager.persistence.wallet.WalletRepositoryImpl;
import br.com.casellisoftware.budgetmanager.persistence.wallet.mappers.WalletPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import({
        WalletRepositoryImpl.class,
        TransactionConfiguration.class,
        TransactionalSaveBulletBoundaryTest.MapperConfig.class,
        TransactionalSaveBulletBoundaryTest.BoundaryConfig.class
})
class TransactionalSaveBulletBoundaryTest extends AbstractMongoIntegrationTest {

    @TestConfiguration
    @ComponentScan(basePackageClasses = WalletPersistenceMapper.class)
    static class MapperConfig {
    }

    @TestConfiguration
    static class BoundaryConfig {

        @Bean
        FindWalletDomainByIdBoundary findWalletDomainByIdBoundary(WalletRepository walletRepository) {
            return new FindWalletDomainByIdUseCase(walletRepository);
        }

        @Bean
        SaveBulletBoundary saveBulletBoundary(WalletRepository walletRepository,
                                              FindWalletDomainByIdBoundary findWalletDomainByIdBoundary) {
            SaveBulletUseCase useCase = new SaveBulletUseCase(
                    new FailingBulletRepository(),
                    walletRepository,
                    findWalletDomainByIdBoundary);
            return new TransactionalSaveBulletBoundary(useCase);
        }
    }

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private SaveBulletBoundary saveBulletBoundary;

    @Test
    void execute_whenBulletSaveFails_rollsBackWalletDebit() {
        Wallet wallet = walletRepository.save(Wallet.create(
                "monthly",
                Money.of("1000.00"),
                null,
                LocalDate.of(2026, 4, 1),
                false
        ));

        assertThatThrownBy(() -> saveBulletBoundary.execute(
                new BulletInput("rent", new BigDecimal("300.00"), wallet.getId())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("forced bullet failure");

        Wallet reloaded = walletRepository.findById(wallet.getId()).orElseThrow();
        assertThat(reloaded.getRemaining()).isEqualTo(Money.of("1000.00"));
    }

    private static final class FailingBulletRepository implements BulletRepository {

        @Override
        public Bullet save(Bullet bullet) {
            throw new IllegalStateException("forced bullet failure");
        }

        @Override
        public Optional<Bullet> findById(String id) {
            return Optional.empty();
        }

        @Override
        public List<Bullet> findAllByIds(List<String> ids) {
            return List.of();
        }

        @Override
        public void deleteById(String id) {
        }
    }
}
