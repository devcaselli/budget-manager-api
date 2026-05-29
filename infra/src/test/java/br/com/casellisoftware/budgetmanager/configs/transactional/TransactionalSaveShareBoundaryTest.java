package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.SaveShareBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareInput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutput;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionalSaveShareBoundaryTest {

    @Test
    void execute_delegatesAndIsAnnotatedTransactional() throws Exception {
        SaveShareBoundary delegate = mock(SaveShareBoundary.class);
        ShareInput input = new ShareInput(
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                new BigDecimal("100.00"),
                "BRL",
                new BigDecimal("40.00"),
                List.of(),
                "owner-1"
        );
        ShareOutput output = mock(ShareOutput.class);
        when(delegate.execute(input)).thenReturn(output);

        TransactionalSaveShareBoundary boundary = new TransactionalSaveShareBoundary(delegate);

        assertThat(boundary.execute(input)).isSameAs(output);
        verify(delegate).execute(input);

        Method method = TransactionalSaveShareBoundary.class.getMethod("execute", ShareInput.class);
        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }
}
