package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.PatchCreditCardInput;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchCreditCardUseCaseTest {

    private static final String OWNER = "owner-1";

    @Mock
    private CreditCardRepository creditCardRepository;

    private PatchCreditCardUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PatchCreditCardUseCase(creditCardRepository);
    }

    @Test
    void execute_labelsProvided_savesWithNewLabels() {
        CreditCard existing = new CreditCard("cc-1", "Nubank", OWNER, List.of());
        CreditCard updated = new CreditCard("cc-1", "Nubank", OWNER, List.of("nubank", "nu"));
        when(creditCardRepository.findById("cc-1", OWNER)).thenReturn(Optional.of(existing));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(updated);

        CreditCardOutput output = useCase.execute(new PatchCreditCardInput("cc-1", OWNER, List.of("nubank", "nu")));

        assertThat(output.labels()).containsExactly("nubank", "nu");
        verify(creditCardRepository).save(any(CreditCard.class));
    }

    @Test
    void execute_labelsNull_doesNotSave_returnsExisting() {
        CreditCard existing = new CreditCard("cc-1", "Nubank", OWNER, List.of("nubank"));
        when(creditCardRepository.findById("cc-1", OWNER)).thenReturn(Optional.of(existing));

        CreditCardOutput output = useCase.execute(new PatchCreditCardInput("cc-1", OWNER, null));

        assertThat(output.labels()).containsExactly("nubank");
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    void execute_cardNotFound_throwsCreditCardNotFoundException() {
        when(creditCardRepository.findById("not-exist", OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new PatchCreditCardInput("not-exist", OWNER, List.of())))
                .isInstanceOf(CreditCardNotFoundException.class);
    }

    @Test
    void execute_nullInput_throwsNPE() {
        assertThatThrownBy(() -> useCase.execute(null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void execute_labelsEmptyList_savesWithEmptyLabels() {
        CreditCard existing = new CreditCard("cc-1", "Nubank", OWNER, List.of("nubank"));
        CreditCard updated = new CreditCard("cc-1", "Nubank", OWNER, List.of());
        when(creditCardRepository.findById("cc-1", OWNER)).thenReturn(Optional.of(existing));
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(updated);

        CreditCardOutput output = useCase.execute(new PatchCreditCardInput("cc-1", OWNER, List.of()));

        assertThat(output.labels()).isEmpty();
        verify(creditCardRepository).save(any(CreditCard.class));
    }
}
