package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindBulletByIdUseCaseTest {

    @Mock
    private BulletRepository bulletRepository;

    private FindBulletByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindBulletByIdUseCase(bulletRepository);
    }

    @Test
    void execute_happyPath_returnsMappedOutput() {
        Money budget = Money.of("500.00");
        Bullet bullet = new Bullet("bullet-1", "rent", budget, budget, "wallet-1");
        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(bullet));

        BulletOutput result = useCase.execute("bullet-1");

        assertThat(result.id()).isEqualTo("bullet-1");
        assertThat(result.description()).isEqualTo("rent");
        assertThat(result.budget()).isEqualByComparingTo("500.00");
        assertThat(result.remaining()).isEqualByComparingTo("500.00");
        assertThat(result.walletId()).isEqualTo("wallet-1");
    }

    @Test
    void execute_notFound_throwsBulletNotFoundException() {
        when(bulletRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("nonexistent"))
                .isInstanceOf(BulletNotFoundException.class)
                .hasMessageContaining("nonexistent");
    }

    @Test
    void execute_repositoryFails_propagates() {
        RuntimeException boom = new RuntimeException("mongo down");
        when(bulletRepository.findById("bullet-1")).thenThrow(boom);

        assertThatThrownBy(() -> useCase.execute("bullet-1")).isSameAs(boom);
    }
}
