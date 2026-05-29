package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardInput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutput;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SaveCreditCardUseCaseTest {

    @Mock
    private CreditCardRepository repository;

    @InjectMocks
    private SaveCreditCardUseCase useCase;

    @Test
    void shouldSaveNewCreditCardSuccessfully() {
        CreditCardInput input = new CreditCardInput("Visa Gold");
        CreditCard creditCard = CreditCard.create("Visa Gold");

        when(repository.save(any(CreditCard.class))).thenReturn(creditCard);

        CreditCardOutput output = useCase.execute(input);

        assertThat(output).isNotNull();
        assertThat(output.id()).isEqualTo(creditCard.getId());
        assertThat(output.name()).isEqualTo("Visa Gold");

        verify(repository, times(1)).save(any(CreditCard.class));
    }

    @Test
    void shouldCallRepositorySaveWithCreatedCreditCard() {
        CreditCardInput input = new CreditCardInput("Mastercard");
        CreditCard savedCard = CreditCard.create("Mastercard");

        when(repository.save(any(CreditCard.class))).thenReturn(savedCard);

        useCase.execute(input);

        verify(repository).save(argThat(card ->
                card.getName().equals("Mastercard") && card.getId() != null
        ));
    }

    @Test
    void shouldThrowExceptionWhenInputIsNull() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void shouldThrowExceptionWhenRepositoryThrows() {
        CreditCardInput input = new CreditCardInput("Amex");

        when(repository.save(any(CreditCard.class)))
                .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
    }

    @Test
    void shouldReturnOutputWithCorrectAssembly() {
        CreditCardInput input = new CreditCardInput("Discover");
        CreditCard savedCard = CreditCard.create("Discover");

        when(repository.save(any(CreditCard.class))).thenReturn(savedCard);

        CreditCardOutput output = useCase.execute(input);

        assertThat(output.id()).isNotBlank();
        assertThat(output.name()).isEqualTo("Discover");
    }
}
