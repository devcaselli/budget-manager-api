package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.PatchReservedBudgetInput;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchReservedBudgetUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final YearMonth JUNE = YearMonth.of(2025, 6);
    private static final YearMonth JULY = YearMonth.of(2025, 7);
    private static final String OWNER = "owner-1";

    @Mock
    private br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository reservedBudgetRepository;

    private PatchReservedBudgetUseCase useCase;

    @BeforeEach
    void setUp() {
        // System clock = June, to reproduce the reported bug exactly.
        Clock clock = Clock.fixed(LocalDate.of(2025, 6, 15).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        useCase = new PatchReservedBudgetUseCase(reservedBudgetRepository, clock);
    }

    private static ReservedBudget rentJune1600() {
        return ReservedBudget.create("Rent", null, BRL, Money.of("1600.00", BRL), JUNE, FlagEnum.NONE, OWNER);
    }

    @Test
    void execute_amountChangeWithFutureEffectiveMonth_doesNotAlterEarlierMonth() {
        ReservedBudget existing = rentJune1600();
        when(reservedBudgetRepository.findById(existing.getId(), OWNER)).thenReturn(Optional.of(existing));
        when(reservedBudgetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Edit to 1400 effective JULY, even though the clock is June.
        PatchReservedBudgetInput input = new PatchReservedBudgetInput(
                existing.getId(), null, null, new BigDecimal("1400.00"), FlagEnum.NONE, JULY, OWNER);

        useCase.execute(input);

        ArgumentCaptor<ReservedBudget> captor = ArgumentCaptor.forClass(ReservedBudget.class);
        verify(reservedBudgetRepository).save(captor.capture());
        ReservedBudget saved = captor.getValue();

        // June must stay 1600; July must become 1400.
        assertThat(saved.resolveAmount(JUNE)).isEqualTo(Money.of("1600.00", BRL));
        assertThat(saved.resolveAmount(JULY)).isEqualTo(Money.of("1400.00", BRL));
        assertThat(saved.getVersions()).hasSize(2);
    }

    @Test
    void execute_amountChangeWithoutEffectiveMonth_fallsBackToClockMonth() {
        ReservedBudget existing = rentJune1600();
        when(reservedBudgetRepository.findById(existing.getId(), OWNER)).thenReturn(Optional.of(existing));
        when(reservedBudgetRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // No effectiveMonth -> falls back to clock (June) -> overwrites June's version.
        PatchReservedBudgetInput input = new PatchReservedBudgetInput(
                existing.getId(), null, null, new BigDecimal("1400.00"), FlagEnum.NONE, null, OWNER);

        useCase.execute(input);

        ArgumentCaptor<ReservedBudget> captor = ArgumentCaptor.forClass(ReservedBudget.class);
        verify(reservedBudgetRepository).save(captor.capture());
        ReservedBudget saved = captor.getValue();

        assertThat(saved.resolveAmount(JUNE)).isEqualTo(Money.of("1400.00", BRL));
        assertThat(saved.getVersions()).hasSize(1);
    }
}
