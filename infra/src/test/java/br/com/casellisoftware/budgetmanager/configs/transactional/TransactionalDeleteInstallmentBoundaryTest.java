package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.installment.boundary.DeleteInstallmentBoundary;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TransactionalDeleteInstallmentBoundaryTest {

    @Test
    void execute_delegatesAndIsTransactional() throws Exception {
        DeleteInstallmentBoundary delegate = mock(DeleteInstallmentBoundary.class);

        TransactionalDeleteInstallmentBoundary boundary = new TransactionalDeleteInstallmentBoundary(delegate);

        boundary.execute("inst-1", "owner-1");

        verify(delegate).execute("inst-1", "owner-1");
        assertThat(executeMethod()).isNotNull();
    }

    private static Transactional executeMethod() throws NoSuchMethodException {
        Method method = TransactionalDeleteInstallmentBoundary.class.getMethod("execute", String.class, String.class);
        return method.getAnnotation(Transactional.class);
    }
}
