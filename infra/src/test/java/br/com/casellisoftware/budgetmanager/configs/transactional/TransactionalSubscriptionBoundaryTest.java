package br.com.casellisoftware.budgetmanager.configs.transactional;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.DeleteSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SaveSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionalSubscriptionBoundaryTest {

    @Test
    void save_execute_delegatesAndIsTransactional() throws Exception {
        SaveSubscriptionBoundary delegate = mock(SaveSubscriptionBoundary.class);
        SubscriptionInput input = new SubscriptionInput("Netflix", new BigDecimal("55.90"), "BRL", null, null, br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum.NONE, "cc-test");
        SubscriptionOutput output = output();
        when(delegate.execute(input)).thenReturn(output);

        TransactionalSaveSubscriptionBoundary boundary = new TransactionalSaveSubscriptionBoundary(delegate);

        assertThat(boundary.execute(input)).isSameAs(output);
        verify(delegate).execute(input);
        assertThat(executeMethod(TransactionalSaveSubscriptionBoundary.class, SubscriptionInput.class))
                .isNotNull();
    }

    @Test
    void patch_execute_delegatesAndIsTransactional() throws Exception {
        PatchSubscriptionBoundary delegate = mock(PatchSubscriptionBoundary.class);
        PatchSubscriptionInput input = new PatchSubscriptionInput("subscription-1", "Netflix Premium", new BigDecimal("60.00"));
        SubscriptionOutput output = output();
        when(delegate.execute(input)).thenReturn(output);

        TransactionalPatchSubscriptionBoundary boundary = new TransactionalPatchSubscriptionBoundary(delegate);

        assertThat(boundary.execute(input)).isSameAs(output);
        verify(delegate).execute(input);
        assertThat(executeMethod(TransactionalPatchSubscriptionBoundary.class, PatchSubscriptionInput.class))
                .isNotNull();
    }

    @Test
    void delete_execute_delegatesAndIsTransactional() throws Exception {
        DeleteSubscriptionBoundary delegate = mock(DeleteSubscriptionBoundary.class);

        TransactionalDeleteSubscriptionBoundary boundary = new TransactionalDeleteSubscriptionBoundary(delegate);

        boundary.execute("subscription-1", "owner-1");

        verify(delegate).execute("subscription-1", "owner-1");
        assertThat(executeMethod(TransactionalDeleteSubscriptionBoundary.class, String.class, String.class))
                .isNotNull();
    }

    private static Transactional executeMethod(Class<?> type, Class<?>... parameterTypes) throws NoSuchMethodException {
        Method method = type.getMethod("execute", parameterTypes);
        return method.getAnnotation(Transactional.class);
    }

    private static SubscriptionOutput output() {
        return new SubscriptionOutput(
                "subscription-1",
                "Netflix",
                "BRL",
                YearMonth.of(2026, 5),
                null,
                List.of()
        );
    }
}
