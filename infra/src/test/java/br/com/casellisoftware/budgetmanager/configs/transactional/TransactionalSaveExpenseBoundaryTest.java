package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.SaveExpenseBoundary;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionalSaveExpenseBoundaryTest {

    @Test
    void execute_delegatesAndIsTransactional() throws Exception {
        SaveExpenseBoundary delegate = mock(SaveExpenseBoundary.class);
        ExpenseInput input = new ExpenseInput(
                "Coffee",
                new BigDecimal("12.34"),
                LocalDate.of(2026, 5, 10),
                "w1",
                "cc1",
                false,
                null,
                FlagEnum.NONE
        );
        ExpenseOutput output = new ExpenseOutput(
                "exp-1",
                "Coffee",
                new BigDecimal("12.34"),
                LocalDate.of(2026, 5, 10),
                "w1",
                "cc1",
                new BigDecimal("12.34"),
                List.of(),
                FlagEnum.NONE
        );
        when(delegate.execute(input)).thenReturn(output);

        TransactionalSaveExpenseBoundary boundary = new TransactionalSaveExpenseBoundary(delegate);

        assertThat(boundary.execute(input)).isSameAs(output);
        verify(delegate).execute(input);
        assertThat(executeMethod()).isNotNull();
    }

    private static Transactional executeMethod() throws NoSuchMethodException {
        Method method = TransactionalSaveExpenseBoundary.class.getMethod("execute", ExpenseInput.class);
        return method.getAnnotation(Transactional.class);
    }
}
