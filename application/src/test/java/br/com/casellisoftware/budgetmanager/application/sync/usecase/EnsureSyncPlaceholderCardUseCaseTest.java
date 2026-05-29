package br.com.casellisoftware.budgetmanager.application.sync.usecase;

import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnsureSyncPlaceholderCardUseCaseTest {

    private static final String OWNER = "owner-1";

    @Mock
    private CreditCardRepository creditCardRepository;

    private EnsureSyncPlaceholderCardUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new EnsureSyncPlaceholderCardUseCase(creditCardRepository);
    }

    @Test
    void ensureFor_placeholderExists_returnsExisting_doesNotSave() {
        CreditCard existing = new CreditCard("card-id", CreditCard.SYNC_PLACEHOLDER_NAME, OWNER);
        when(creditCardRepository.findByName(CreditCard.SYNC_PLACEHOLDER_NAME, OWNER))
                .thenReturn(Optional.of(existing));

        CreditCard result = useCase.ensureFor(OWNER);

        assertThat(result.getId()).isEqualTo("card-id");
        verify(creditCardRepository, never()).save(any());
    }

    @Test
    void ensureFor_placeholderAbsent_createsAndSaves() {
        CreditCard created = new CreditCard("new-id", CreditCard.SYNC_PLACEHOLDER_NAME, OWNER);
        when(creditCardRepository.findByName(CreditCard.SYNC_PLACEHOLDER_NAME, OWNER))
                .thenReturn(Optional.empty());
        when(creditCardRepository.save(any(CreditCard.class))).thenReturn(created);

        CreditCard result = useCase.ensureFor(OWNER);

        assertThat(result.getId()).isEqualTo("new-id");
        assertThat(result.getName()).isEqualTo(CreditCard.SYNC_PLACEHOLDER_NAME);
        verify(creditCardRepository).save(any(CreditCard.class));
    }
}
