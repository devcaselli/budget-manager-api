package br.com.casellisoftware.budgetmanager.persistence.bullet;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.persistence.bullet.mappers.BulletPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import({BulletRepositoryImpl.class, BulletRepositoryImplTest.MapperConfig.class})
class BulletRepositoryImplTest extends AbstractMongoIntegrationTest {

    @TestConfiguration
    @ComponentScan(basePackageClasses = BulletPersistenceMapper.class)
    static class MapperConfig {}

    @Autowired
    private BulletRepositoryImpl repository;

    private static Bullet newBullet(String description, String amount, String walletId) {
        Money budget = Money.of(amount);
        return Bullet.create(description, budget, budget, walletId);
    }

    @Test
    void save_persistsAllFields() {
        Bullet saved = repository.save(newBullet("rent", "1500.00", "wallet-1"));

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getDescription()).isEqualTo("rent");
        assertThat(saved.getBudget().amount()).isEqualByComparingTo("1500.00");
        assertThat(saved.getRemaining().amount()).isEqualByComparingTo("1500.00");
        assertThat(saved.getWalletId()).isEqualTo("wallet-1");
    }

    @Test
    void findById_whenFound_returnsMappedBullet() {
        Bullet saved = repository.save(newBullet("groceries", "300.00", "wallet-1"));

        Optional<Bullet> result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getDescription()).isEqualTo("groceries");
        assertThat(result.get().getBudget().amount()).isEqualByComparingTo("300.00");
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        assertThat(repository.findById("nonexistent-id")).isEmpty();
    }

    @Test
    void findByWalletId_returnsBulletsForWallet() {
        repository.save(newBullet("rent", "1500.00", "wallet-1"));
        repository.save(newBullet("food", "300.00", "wallet-1"));
        repository.save(newBullet("other", "200.00", "wallet-2"));

        List<Bullet> result = repository.findByWalletId("wallet-1");

        assertThat(result).hasSize(2);
        assertThat(result).allMatch(b -> b.getWalletId().equals("wallet-1"));
    }

    @Test
    void findByWalletId_filtersToCorrectWallet() {
        repository.save(newBullet("savings", "500.00", "wallet-2"));

        List<Bullet> result = repository.findByWalletId("wallet-1");

        assertThat(result).isEmpty();
    }

    @Test
    void findByWalletId_noResults_returnsEmptyList() {
        List<Bullet> result = repository.findByWalletId("nonexistent-wallet");

        assertThat(result).isEmpty();
    }
}
