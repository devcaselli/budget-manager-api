package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.usecase.SaveExpenseUseCase;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveExpenseUseCaseTest {

    @Mock
    private ExpenseRepository expenseRepository;

    private SaveExpenseUseCase useCase;

    private ExpenseInput input;

    @BeforeEach
    void setUp() {
        useCase = new SaveExpenseUseCase(expenseRepository);
        input = new ExpenseInput(
                "lunch",
                new BigDecimal("25.50"),
                LocalDate.now().minusDays(1),
                "wallet-1"
        );
    }

    @Test
    void execute_happyPath_createsDomainPersistsAndReturnsOutput() {
        when(expenseRepository.save(any(Expense.class))).thenAnswer(inv -> inv.getArgument(0));

        ExpenseOutput result = useCase.execute(input);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        org.mockito.Mockito.verify(expenseRepository).save(captor.capture());

        Expense persisted = captor.getValue();
        assertThat(persisted.getId()).isNotBlank();
        assertThat(persisted.getWalletId()).isEqualTo("wallet-1");
        assertThat(persisted.getName()).isEqualTo("lunch");
        assertThat(persisted.getCost().amount()).isEqualByComparingTo("25.50");
        assertThat(persisted.getRemaining().amount()).isEqualByComparingTo("25.50");

        assertThat(result.id()).isEqualTo(persisted.getId());
        assertThat(result.name()).isEqualTo("lunch");
        assertThat(result.cost()).isEqualByComparingTo("25.50");
        assertThat(result.remaining()).isEqualByComparingTo("25.50");
        assertThat(result.walletId()).isEqualTo("wallet-1");
        assertThat(result.purchaseDate()).isEqualTo(input.purchaseDate());
    }

    @Test
    void execute_whenRepositoryFails_propagates() {
        RuntimeException boom = new RuntimeException("mongo down");
        when(expenseRepository.save(any(Expense.class))).thenThrow(boom);

        assertThatThrownBy(() -> useCase.execute(input)).isSameAs(boom);
    }

    @Test
    void execute_whenDomainRejectsFuturePurchaseDate_doesNotTouchRepository() {
        ExpenseInput invalid = new ExpenseInput(
                "lunch", new BigDecimal("25.50"), LocalDate.now().plusDays(1), "wallet-1");

        assertThatThrownBy(() -> useCase.execute(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("future");

        verifyNoInteractions(expenseRepository);
    }
}
