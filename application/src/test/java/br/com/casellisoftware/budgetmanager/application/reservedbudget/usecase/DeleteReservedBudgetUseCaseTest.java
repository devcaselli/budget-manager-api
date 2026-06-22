package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteReservedBudgetUseCaseTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final YearMonth MARCH = YearMonth.of(2025, 3);

    @Mock
    private ReservedBudgetRepository reservedBudgetRepository;

    private DeleteReservedBudgetUseCase useCase;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(LocalDate.of(2025, 9, 1).atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
        useCase = new DeleteReservedBudgetUseCase(reservedBudgetRepository, clock);
    }

    private static ReservedBudget activeReservedBudget() {
        return ReservedBudget.create("Aluguel", null, BRL, Money.of("2000.00", BRL), MARCH, FlagEnum.NONE, "owner-1");
    }

    @Test
    void execute_notFound_throws() {
        when(reservedBudgetRepository.findById("rb-1", "owner-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("rb-1", "owner-1"))
                .isInstanceOf(ReservedBudgetNotFoundException.class);

        verify(reservedBudgetRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void execute_marksDeletedAndSaves() {
        ReservedBudget existing = activeReservedBudget();
        when(reservedBudgetRepository.findById(existing.getId(), "owner-1")).thenReturn(Optional.of(existing));

        useCase.execute(existing.getId(), "owner-1");

        ArgumentCaptor<ReservedBudget> captor = ArgumentCaptor.forClass(ReservedBudget.class);
        verify(reservedBudgetRepository).save(captor.capture());
        ReservedBudget saved = captor.getValue();
        assertThat(saved.isDeleted()).isTrue();
        assertThat(saved.getDeletedAt()).isEqualTo(LocalDateTime.of(2025, 9, 1, 0, 0));
    }

    @Test
    void execute_alreadyDeleted_isNoOp() {
        ReservedBudget alreadyDeleted = activeReservedBudget().markDeleted(LocalDateTime.of(2025, 5, 1, 0, 0));
        when(reservedBudgetRepository.findById(alreadyDeleted.getId(), "owner-1")).thenReturn(Optional.of(alreadyDeleted));

        useCase.execute(alreadyDeleted.getId(), "owner-1");

        verify(reservedBudgetRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
