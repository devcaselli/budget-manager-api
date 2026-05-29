package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.flag.FlagAwareExecutor;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveBulletUseCaseFlagDispatchTest {

    @Mock
    private FlagAwareExecutor<BulletInput, BulletOutput> executor;

    @Mock
    private FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    private SaveBulletUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SaveBulletUseCase(executor, findWalletDomainByIdBoundary);
    }

    @Test
    void execute_dispatchesToExecutorUsingWalletFlag() {
        BulletInput input = new BulletInput(
                "rent",
                new BigDecimal("1500.00"),
                "wallet-1",
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION
        );
        Wallet wallet = Wallet.create(
                "Monthly",
                Money.of("5000.00"),
                null,
                LocalDate.of(2026, 4, 1),
                false,
                YearMonth.of(2026, 4),
                WalletState.PRODUCTION,
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION
        );
        BulletOutput expected = new BulletOutput(
                "bullet-1",
                "rent",
                new BigDecimal("1500.00"),
                new BigDecimal("1500.00"),
                "wallet-1",
                FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION
        );

        when(findWalletDomainByIdBoundary.findById("wallet-1", "legacy")).thenReturn(wallet);
        when(executor.execute(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, input)).thenReturn(expected);

        BulletOutput output = useCase.execute(input);

        assertThat(output).isEqualTo(expected);
        verify(findWalletDomainByIdBoundary).findById("wallet-1", "legacy");
        verify(executor).execute(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, input);
    }
}
