package br.com.casellisoftware.budgetmanager.domain;

import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.assertj.core.api.Assertions.assertThat;

class NotFoundExceptionContractTest {

    @Test
    void notFoundExceptions_haveSingleStringIdConstructor() {
        assertSingleStringConstructor(BulletNotFoundException.class);
        assertSingleStringConstructor(ExpenseNotFoundException.class);
        assertSingleStringConstructor(PaymentNotFoundException.class);
        assertSingleStringConstructor(WalletNotFoundException.class);
    }

    @Test
    void notFoundExceptions_wrapIdIntoStandardMessage() {
        assertThat(new BulletNotFoundException("bullet-1"))
                .hasMessage("Bullet not found: bullet-1");
        assertThat(new ExpenseNotFoundException("expense-1"))
                .hasMessage("Expense not found: expense-1");
        assertThat(new PaymentNotFoundException("payment-1"))
                .hasMessage("Payment not found: payment-1");
        assertThat(new WalletNotFoundException("wallet-1"))
                .hasMessage("Wallet not found: wallet-1");
    }

    private static void assertSingleStringConstructor(Class<? extends RuntimeException> exceptionType) {
        Constructor<?>[] constructors = exceptionType.getDeclaredConstructors();

        assertThat(constructors).hasSize(1);
        assertThat(constructors[0].getParameterTypes()).containsExactly(String.class);
    }
}
