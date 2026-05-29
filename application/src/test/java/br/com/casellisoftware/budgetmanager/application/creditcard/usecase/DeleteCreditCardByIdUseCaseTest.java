package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardInUseException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteCreditCardByIdUseCaseTest {

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @InjectMocks
    private DeleteCreditCardByIdUseCase useCase;

    @Test
    void execute_cardNotFound_throwsAndDoesNotDelete() {
        when(creditCardRepository.existsById(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute("missing", "owner-1"))
                .isInstanceOf(CreditCardNotFoundException.class)
                .hasMessageContaining("missing");

        verify(creditCardRepository, never()).deleteById("missing");
    }

    @Test
    void execute_referencesPresent_throwsCreditCardInUseException() {
        when(creditCardRepository.existsById("cc1", "owner-1")).thenReturn(true);
        when(expenseRepository.findIdsByCreditCardId("cc1", "owner-1")).thenReturn(List.of("exp-1"));
        when(installmentRepository.findIdsByCreditCardId("cc1", "owner-1")).thenReturn(List.of("inst-1"));

        CreditCardInUseException exception = catchThrowableOfType(
                () -> useCase.execute("cc1", "owner-1"),
                CreditCardInUseException.class
        );

        assertThat(exception).hasMessageContaining("cc1");
        assertThat(exception.getExpenseIds()).containsExactly("exp-1");
        assertThat(exception.getInstallmentIds()).containsExactly("inst-1");

        verify(creditCardRepository, never()).deleteById("cc1");
    }

    @Test
    void execute_noReferences_deletesCreditCard() {
        when(creditCardRepository.existsById("cc1", "owner-1")).thenReturn(true);
        when(expenseRepository.findIdsByCreditCardId("cc1", "owner-1")).thenReturn(List.of());
        when(installmentRepository.findIdsByCreditCardId("cc1", "owner-1")).thenReturn(List.of());

        useCase.execute("cc1", "owner-1");

        verify(creditCardRepository).deleteById("cc1", "owner-1");
    }

    @Test
    void execute_deletedInstallmentReferenceStillBlocksDeletion() {
        when(creditCardRepository.existsById("cc1", "owner-1")).thenReturn(true);
        when(expenseRepository.findIdsByCreditCardId("cc1", "owner-1")).thenReturn(List.of());
        when(installmentRepository.findIdsByCreditCardId("cc1", "owner-1")).thenReturn(List.of("inst-deleted"));

        CreditCardInUseException exception = catchThrowableOfType(
                () -> useCase.execute("cc1", "owner-1"),
                CreditCardInUseException.class
        );

        assertThat(exception.getExpenseIds()).isEmpty();
        assertThat(exception.getInstallmentIds()).containsExactly("inst-deleted");

        verify(creditCardRepository, never()).deleteById("cc1");
    }

    @Test
    void execute_activeSubscriptionReferenceBlocksDeletion() {
        when(creditCardRepository.existsById("cc1", "owner-1")).thenReturn(true);
        when(expenseRepository.findIdsByCreditCardId("cc1", "owner-1")).thenReturn(List.of());
        when(installmentRepository.findIdsByCreditCardId("cc1", "owner-1")).thenReturn(List.of());
        when(subscriptionRepository.existsActiveByCreditCardId("cc1", "owner-1")).thenReturn(true);

        CreditCardInUseException exception = catchThrowableOfType(
                () -> useCase.execute("cc1", "owner-1"),
                CreditCardInUseException.class
        );

        assertThat(exception.hasActiveSubscription()).isTrue();
        assertThat(exception.getExpenseIds()).isEmpty();
        assertThat(exception.getInstallmentIds()).isEmpty();
        verify(creditCardRepository, never()).deleteById("cc1");
    }

    @Test
    void execute_nullId_throwsImmediately() {
        assertThatThrownBy(() -> useCase.execute(null, "owner-1"))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("id must not be null");

        verify(creditCardRepository, never()).existsById(null);
    }
}
