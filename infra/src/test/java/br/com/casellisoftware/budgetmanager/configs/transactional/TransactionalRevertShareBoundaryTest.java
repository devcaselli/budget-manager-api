package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.RevertShareBoundary;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TransactionalRevertShareBoundaryTest {

    @Test
    void execute_delegatesAndIsAnnotatedTransactional() throws Exception {
        RevertShareBoundary delegate = mock(RevertShareBoundary.class);

        TransactionalRevertShareBoundary boundary = new TransactionalRevertShareBoundary(delegate);
        boundary.execute("share-1", "owner-1");

        verify(delegate).execute("share-1", "owner-1");

        Method method = TransactionalRevertShareBoundary.class.getMethod("execute", String.class, String.class);
        assertThat(method.getAnnotation(Transactional.class)).isNotNull();
    }
}
