package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutput;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindCreditCardByIdUseCaseTest {

    private static final String OWNER_ID = "owner-1";

    @Mock
    private CreditCardRepository repository;

    @InjectMocks
    private FindCreditCardByIdUseCase useCase;

    @Test
    void shouldFindCreditCardByIdSuccessfully() {
        String id = "test-id-123";
        CreditCard creditCard = new CreditCard(id, "Visa Gold");

        when(repository.findById(id, OWNER_ID)).thenReturn(Optional.of(creditCard));

        CreditCardOutput output = useCase.findById(id, OWNER_ID);

        assertThat(output).isNotNull();
        assertThat(output.id()).isEqualTo(id);
        assertThat(output.name()).isEqualTo("Visa Gold");

        verify(repository, times(1)).findById(id, OWNER_ID);
    }

    @Test
    void shouldThrowCreditCardNotFoundExceptionWhenIdDoesNotExist() {
        String id = "non-existent-id";

        when(repository.findById(id, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById(id, OWNER_ID))
                .isInstanceOf(CreditCardNotFoundException.class)
                .hasMessageContaining(id);

        verify(repository, times(1)).findById(id, OWNER_ID);
    }

    @Test
    void shouldCallRepositoryWithCorrectId() {
        String id = "card-456";
        CreditCard creditCard = new CreditCard(id, "Mastercard");

        when(repository.findById(id, OWNER_ID)).thenReturn(Optional.of(creditCard));

        useCase.findById(id, OWNER_ID);

        verify(repository).findById(id, OWNER_ID);
    }

    @Test
    void shouldThrowExceptionWhenCardBelongsToDifferentOwner() {
        String id = "card-789";

        when(repository.findById(id, "other-owner")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById(id, "other-owner"))
                .isInstanceOf(CreditCardNotFoundException.class);
    }

    @Test
    void shouldThrowExceptionWhenIdIsBlank() {
        when(repository.findById("", OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.findById("", OWNER_ID))
                .isInstanceOf(CreditCardNotFoundException.class);
    }

    @Test
    void shouldReturnOutputWithCorrectAssembly() {
        String id = "amex-789";
        String name = "American Express";
        CreditCard creditCard = new CreditCard(id, name);

        when(repository.findById(id, OWNER_ID)).thenReturn(Optional.of(creditCard));

        CreditCardOutput output = useCase.findById(id, OWNER_ID);

        assertThat(output.id()).isEqualTo(id);
        assertThat(output.name()).isEqualTo(name);
    }

    @Test
    void shouldThrowExceptionWhenRepositoryThrows() {
        String id = "error-id";

        when(repository.findById(id, OWNER_ID))
                .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> useCase.findById(id, OWNER_ID))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
    }
}
