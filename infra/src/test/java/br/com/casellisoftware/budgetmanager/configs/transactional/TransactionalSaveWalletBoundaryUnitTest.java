package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.SaveWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionalSaveWalletBoundaryUnitTest {

    @Test
    void execute_delegatesAndIsTransactional() throws Exception {
        SaveWalletBoundary delegate = mock(SaveWalletBoundary.class);
        WalletInput input = new WalletInput(
                "May",
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 5, 1),
                null,
                false,
                null,
                null
        );
        WalletOutput output = new WalletOutput(
                "wallet-1",
                "May",
                new BigDecimal("1000.00"),
                new BigDecimal("1000.00"),
                LocalDate.of(2026, 5, 1),
                null,
                false,
                null,
                null
        );
        when(delegate.execute(input)).thenReturn(output);

        TransactionalSaveWalletBoundary boundary = new TransactionalSaveWalletBoundary(delegate);

        assertThat(boundary.execute(input)).isSameAs(output);
        verify(delegate).execute(input);
        assertThat(executeMethod()).isNotNull();
    }

    private static Transactional executeMethod() throws NoSuchMethodException {
        Method method = TransactionalSaveWalletBoundary.class.getMethod("execute", WalletInput.class);
        return method.getAnnotation(Transactional.class);
    }
}
