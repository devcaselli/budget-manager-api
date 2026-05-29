package br.com.casellisoftware.budgetmanager.application.installment.usecase;

import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindInstallmentByIdUseCaseTest {

    private static final String OWNER = "owner-1";

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private ShareRepository shareRepository;

    private FindInstallmentByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindInstallmentByIdUseCase(installmentRepository, shareRepository);
    }

    @Test
    void findById_whenFound_returnsMappedOutput() {
        Installment installment = installment("inst-1", "wallet-1", YearMonth.of(2026, 5));
        when(installmentRepository.findById("inst-1", OWNER)).thenReturn(Optional.of(installment));
        when(shareRepository.findActiveBySourceId(ShareSourceType.INSTALLMENT, "inst-1", OWNER)).thenReturn(Optional.empty());

        InstallmentOutput output = useCase.findById("inst-1", OWNER);

        assertThat(output.id()).isEqualTo("inst-1");
        assertThat(output.description()).isEqualTo("Notebook");
        assertThat(output.installmentValue()).isEqualByComparingTo("1000.00");
        assertThat(output.creditCardId()).isEqualTo("cc1");
        assertThat(output.sourceWalletId()).isEqualTo("wallet-1");
        assertThat(output.deleted()).isFalse();
    }

    @Test
    void findById_whenMissing_throws() {
        when(installmentRepository.findById("missing", OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById("missing", OWNER))
                .isInstanceOf(InstallmentNotFoundException.class)
                .hasMessageContaining("missing");
    }

    private static Installment installment(String id, String walletId, YearMonth sourceMonth) {
        return Installment.rebuild(
                id,
                "Notebook",
                Money.of(new BigDecimal("6000.00")),
                Money.of(new BigDecimal("1000.00")),
                6,
                LocalDate.of(2026, 5, 10),
                YearMonth.of(2026, 10),
                "cc1",
                walletId,
                sourceMonth,
                false,
                null,
                FlagEnum.NONE
        );
    }
}
