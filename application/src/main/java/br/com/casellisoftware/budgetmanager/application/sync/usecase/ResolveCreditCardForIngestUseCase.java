package br.com.casellisoftware.budgetmanager.application.sync.usecase;

import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.LabelNormalizer;
import br.com.casellisoftware.budgetmanager.domain.sync.PendingExpense;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Resolves which {@link CreditCard} should receive an ingest-sync expense.
 *
 * <p>Strategy:
 * <ol>
 *   <li>Normalize the {@code cardLabel} from the pending expense.</li>
 *   <li>Lookup the owner's credit cards by normalized label.</li>
 *   <li>If a match is found, return it.</li>
 *   <li>Otherwise, return (or create) the {@code card_sync} placeholder via
 *       {@link EnsureSyncPlaceholderCardUseCase}.</li>
 * </ol>
 */
public class ResolveCreditCardForIngestUseCase {

    private static final Logger log = LoggerFactory.getLogger(ResolveCreditCardForIngestUseCase.class);

    private final CreditCardRepository creditCardRepository;
    private final EnsureSyncPlaceholderCardUseCase ensurePlaceholder;

    public ResolveCreditCardForIngestUseCase(CreditCardRepository creditCardRepository,
                                             EnsureSyncPlaceholderCardUseCase ensurePlaceholder) {
        this.creditCardRepository = Objects.requireNonNull(creditCardRepository, "creditCardRepository must not be null");
        this.ensurePlaceholder = Objects.requireNonNull(ensurePlaceholder, "ensurePlaceholder must not be null");
    }

    /**
     * Resolves the credit card for a pending expense.
     *
     * @return matched credit card or the {@code card_sync} placeholder; never null
     */
    public ResolvedCard resolve(PendingExpense pending) {
        String normalizedLabel = LabelNormalizer.normalize(pending.cardLabel());

        if (!normalizedLabel.isBlank()) {
            var matched = creditCardRepository.findByNormalizedLabel(normalizedLabel, pending.ownerId());
            if (matched.isPresent()) {
                log.debug("Matched cardLabel='{}' to creditCardId={} ownerId={}", pending.cardLabel(), matched.get().getId(), pending.ownerId());
                return new ResolvedCard(matched.get(), false);
            }
        }

        log.info("No label match for cardLabel='{}' ownerId={} — using card_sync placeholder", pending.cardLabel(), pending.ownerId());
        CreditCard placeholder = ensurePlaceholder.ensureFor(pending.ownerId());
        return new ResolvedCard(placeholder, true);
    }

    /**
     * Result of credit card resolution.
     *
     * @param card       resolved credit card
     * @param isFallback {@code true} if the placeholder {@code card_sync} was used
     */
    public record ResolvedCard(CreditCard card, boolean isFallback) {
    }
}
