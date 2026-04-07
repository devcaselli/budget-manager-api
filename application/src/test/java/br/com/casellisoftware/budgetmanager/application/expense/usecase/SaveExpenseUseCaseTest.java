package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.mappers.ExpenseApplicationMapper;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveExpenseUseCaseTest {

    @Mock
    private ExpenseApplicationMapper mapper;

    @Mock
    private ExpenseRepository expenseRepository;

    private SaveExpenseUseCase useCase;

    private ExpenseInput input;
    private Expense domain;
    private Expense saved;
    private ExpenseOutput output;

    @BeforeEach
    void setUp() {
        useCase = new SaveExpenseUseCase(mapper, expenseRepository);

        input = new ExpenseInput("lunch", new BigDecimal("25.50"), Instant.parse("2026-04-07T12:00:00Z"), "wallet-1");
        domain = new Expense(null, "lunch", new BigDecimal("25.50"), Instant.parse("2026-04-07T12:00:00Z"), null, "wallet-1");
        saved = new Expense("expense-1", "lunch", new BigDecimal("25.50"), Instant.parse("2026-04-07T12:00:00Z"), null, "wallet-1");
        output = new ExpenseOutput("expense-1", "lunch", new BigDecimal("25.50"), Instant.parse("2026-04-07T12:00:00Z"), "wallet-1", null);
    }

    @Test
    void execute_happyPath_mapsPersistsAndReturnsOutput() {
        when(mapper.mapToDomain(input)).thenReturn(domain);
        when(expenseRepository.save(domain)).thenReturn(saved);
        when(mapper.mapToOutput(saved)).thenReturn(output);

        ExpenseOutput result = useCase.execute(input);

        assertThat(result).isSameAs(output);

        InOrder order = inOrder(mapper, expenseRepository);
        order.verify(mapper).mapToDomain(input);
        order.verify(expenseRepository).save(domain);
        order.verify(mapper).mapToOutput(saved);
    }

    @Test
    void execute_whenRepositoryFails_propagatesAndDoesNotMapOutput() {
        when(mapper.mapToDomain(input)).thenReturn(domain);
        RuntimeException boom = new RuntimeException("mongo down");
        when(expenseRepository.save(domain)).thenThrow(boom);

        assertThatThrownBy(() -> useCase.execute(input)).isSameAs(boom);

        verify(mapper, never()).mapToOutput(any());
    }

    @Test
    void execute_whenMapToDomainFails_doesNotTouchRepository() {
        IllegalStateException boom = new IllegalStateException("mapping failed");
        when(mapper.mapToDomain(input)).thenThrow(boom);

        assertThatThrownBy(() -> useCase.execute(input)).isSameAs(boom);

        verifyNoInteractions(expenseRepository);
    }
}
