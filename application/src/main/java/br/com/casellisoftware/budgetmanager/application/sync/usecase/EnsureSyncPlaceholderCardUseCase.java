package br.com.casellisoftware.budgetmanager.application.sync.usecase;

import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Gets or creates the {@code card_sync} placeholder credit card for an owner.
 *
 * <p>The placeholder is used when no CreditCard label matches the SMS-extracted
 * card label from ingest-api. All unmatched expenses are attributed to this card.</p>
 *
 * <p>Concurrency safety: the infra repository implementation uses a Mongo upsert
 * ({@code $setOnInsert}) with a unique index on {@code (ownerId, name)}, ensuring
 * exactly one placeholder is created even under concurrent sync runs.</p>
 */
public class EnsureSyncPlaceholderCardUseCase {

    private static final Logger log = LoggerFactory.getLogger(EnsureSyncPlaceholderCardUseCase.class);

    private final CreditCardRepository creditCardRepository;

    public EnsureSyncPlaceholderCardUseCase(CreditCardRepository creditCardRepository) {
        this.creditCardRepository = Objects.requireNonNull(creditCardRepository, "creditCardRepository must not be null");
    }

    /**
     * Returns the existing {@code card_sync} placeholder for the owner, creating it if absent.
     *
     * @param ownerId owner identifier
     * @return the placeholder CreditCard (never null)
     */
    public CreditCard ensureFor(String ownerId) {
        return creditCardRepository.findByName(CreditCard.SYNC_PLACEHOLDER_NAME, ownerId)
                .orElseGet(() -> {
                    log.info("Creating card_sync placeholder for ownerId={}", ownerId);
                    CreditCard placeholder = CreditCard.create(CreditCard.SYNC_PLACEHOLDER_NAME, ownerId);
                    return creditCardRepository.save(placeholder);
                });
    }
}
