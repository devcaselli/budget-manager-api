package br.com.casellisoftware.budgetmanager.domain.expense;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExpenseNotFoundExceptionTest {

    @Test
    void message_includesExpenseId() {
        ExpenseNotFoundException ex = new ExpenseNotFoundException("abc-123");
        assertThat(ex.getMessage()).contains("abc-123");
    }

    @Test
    void isRuntimeException() {
        assertThat(new ExpenseNotFoundException("x")).isInstanceOf(RuntimeException.class);
    }
}
