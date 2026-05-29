package br.com.casellisoftware.budgetmanager.application.sync.usecase;

import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.sync.PendingExpense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResolveCreditCardForIngestUseCaseTest {

    private static final String OWNER = "owner-1";

    @Mock
    private CreditCardRepository creditCardRepository;

    @Mock
    private EnsureSyncPlaceholderCardUseCase ensurePlaceholder;

    private ResolveCreditCardForIngestUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ResolveCreditCardForIngestUseCase(creditCardRepository, ensurePlaceholder);
    }

    private PendingExpense pending(String cardLabel) {
        return new PendingExpense("pending-1", OWNER, "Nubank", "1234",
                cardLabel, BigDecimal.valueOf(100), "BRL", "Merchant", Instant.now());
    }

    @Test
    void resolve_labelMatches_returnsCreditCard_isFallbackFalse() {
        CreditCard matched = new CreditCard("cc-1", "Nubank", OWNER);
        when(creditCardRepository.findByNormalizedLabel("nubank", OWNER))
                .thenReturn(Optional.of(matched));

        ResolveCreditCardForIngestUseCase.ResolvedCard result = useCase.resolve(pending("NUBANK"));

        assertThat(result.card().getId()).isEqualTo("cc-1");
        assertThat(result.isFallback()).isFalse();
        verify(ensurePlaceholder, never()).ensureFor(OWNER);
    }

    @Test
    void resolve_labelMatchesAccented_returnsCreditCard() {
        CreditCard matched = new CreditCard("cc-2", "Bradéscard", OWNER);
        when(creditCardRepository.findByNormalizedLabel("bradescard", OWNER))
                .thenReturn(Optional.of(matched));

        ResolveCreditCardForIngestUseCase.ResolvedCard result = useCase.resolve(pending("BRADÉSCARD "));

        assertThat(result.card().getId()).isEqualTo("cc-2");
        assertThat(result.isFallback()).isFalse();
    }

    @Test
    void resolve_noLabelMatch_usesPlaceholder_isFallbackTrue() {
        CreditCard placeholder = new CreditCard("ph-1", CreditCard.SYNC_PLACEHOLDER_NAME, OWNER);
        when(creditCardRepository.findByNormalizedLabel("unknownbank", OWNER))
                .thenReturn(Optional.empty());
        when(ensurePlaceholder.ensureFor(OWNER)).thenReturn(placeholder);

        ResolveCreditCardForIngestUseCase.ResolvedCard result = useCase.resolve(pending("unknownbank"));

        assertThat(result.card().getId()).isEqualTo("ph-1");
        assertThat(result.isFallback()).isTrue();
    }

    @Test
    void resolve_nullCardLabel_usesPlaceholder_isFallbackTrue() {
        CreditCard placeholder = new CreditCard("ph-1", CreditCard.SYNC_PLACEHOLDER_NAME, OWNER);
        when(ensurePlaceholder.ensureFor(OWNER)).thenReturn(placeholder);

        ResolveCreditCardForIngestUseCase.ResolvedCard result = useCase.resolve(pending(null));

        assertThat(result.isFallback()).isTrue();
        verify(creditCardRepository, never()).findByNormalizedLabel(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void resolve_blankCardLabel_usesPlaceholder_isFallbackTrue() {
        CreditCard placeholder = new CreditCard("ph-1", CreditCard.SYNC_PLACEHOLDER_NAME, OWNER);
        when(ensurePlaceholder.ensureFor(OWNER)).thenReturn(placeholder);

        ResolveCreditCardForIngestUseCase.ResolvedCard result = useCase.resolve(pending("  "));

        assertThat(result.isFallback()).isTrue();
    }
}
