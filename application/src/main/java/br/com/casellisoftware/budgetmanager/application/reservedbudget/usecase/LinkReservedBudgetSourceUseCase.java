package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.LinkReservedBudgetSourceBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.LinkReservedBudgetSourceInput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutputAssembler;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLink;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkConflictException;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Links a subscription or installment to a reserved budget.
 *
 * <p>Invariants enforced (in order):</p>
 * <ol>
 *   <li>RB exists and is owned by {@code ownerId} (404).</li>
 *   <li>Source item exists, is owned by {@code ownerId}, and is not deleted (404).</li>
 *   <li>Item currency matches RB currency (400).</li>
 *   <li>Cardinality: item not already linked to a <em>different</em> RB (409).
 *       Re-linking to the <em>same</em> RB replaces the existing link.</li>
 *   <li>Cap: speculative links (existing + new) do not exceed RB ceiling in any month (422).</li>
 *   <li>Save.</li>
 * </ol>
 *
 * <p>Note on concurrency: the cardinality check + save are not globally atomic across
 * different RBs. For a single-user system this residual risk is acceptable and is
 * documented by design.</p>
 */
public class LinkReservedBudgetSourceUseCase implements LinkReservedBudgetSourceBoundary {

    private static final Logger log = LoggerFactory.getLogger(LinkReservedBudgetSourceUseCase.class);

    private final ReservedBudgetRepository reservedBudgetRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final InstallmentRepository installmentRepository;
    private final ReservedBudgetLinkValidationService validationService;

    public LinkReservedBudgetSourceUseCase(ReservedBudgetRepository reservedBudgetRepository,
                                           SubscriptionRepository subscriptionRepository,
                                           InstallmentRepository installmentRepository,
                                           ReservedBudgetLinkValidationService validationService) {
        this.reservedBudgetRepository = Objects.requireNonNull(reservedBudgetRepository);
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
        this.installmentRepository = Objects.requireNonNull(installmentRepository);
        this.validationService = Objects.requireNonNull(validationService);
    }

    @Override
    public ReservedBudgetOutput execute(LinkReservedBudgetSourceInput input) {
        Objects.requireNonNull(input, "input must not be null");
        log.info("Linking {} '{}' to reserved budget '{}' from {}",
                input.sourceType(), input.sourceId(), input.reservedBudgetId(), input.fromMonth());

        // 1. Load and ownership-check the RB
        ReservedBudget rb = reservedBudgetRepository.findById(input.reservedBudgetId(), input.ownerId())
                .orElseThrow(() -> new ReservedBudgetNotFoundException(input.reservedBudgetId()));

        // 2. Load and ownership-check the source item; verify currency
        Money itemCurrency = resolveItemCurrency(input);
        validateCurrency(rb, itemCurrency, input);

        // 3. Cardinality: check if item is already linked elsewhere
        Optional<ReservedBudget> existingRb = reservedBudgetRepository.findByLinkedSource(
                input.sourceType(), input.sourceId(), input.ownerId());

        if (existingRb.isPresent() && !existingRb.get().getId().equals(rb.getId())) {
            throw new ReservedBudgetLinkConflictException(
                    input.sourceType(), input.sourceId(), existingRb.get().getId());
        }

        // 4. Build speculative link list (replace if same key, add if new)
        ReservedBudgetLink newLink = new ReservedBudgetLink(
                input.sourceType(), input.sourceId(), input.fromMonth());

        List<ReservedBudgetLink> speculative = buildSpeculativeLinks(rb, newLink);

        // 5. Cap validation against speculative set
        validationService.validate(rb, speculative, input.ownerId());

        // 6. Persist
        ReservedBudget saved = reservedBudgetRepository.save(rb.addLink(newLink));
        log.info("Link created: {} '{}' → reserved budget '{}'",
                input.sourceType(), input.sourceId(), saved.getId());

        return ReservedBudgetOutputAssembler.from(saved);
    }

    /**
     * Loads the source item to verify it exists, is owned by the caller, is not deleted,
     * and returns its currency for the cross-currency check.
     */
    private Money resolveItemCurrency(LinkReservedBudgetSourceInput input) {
        if (input.sourceType() == ReservedBudgetLinkSourceType.SUBSCRIPTION) {
            Subscription sub = subscriptionRepository.findById(input.sourceId(), input.ownerId())
                    .orElseThrow(() -> new SubscriptionNotFoundException(input.sourceId()));
            return sub.resolveAmount(input.fromMonth());
        } else {
            Installment inst = installmentRepository.findById(input.sourceId(), input.ownerId())
                    .orElseThrow(() -> new InstallmentNotFoundException(input.sourceId()));
            if (inst.isDeleted()) {
                throw new InstallmentNotFoundException(input.sourceId());
            }
            return inst.installmentValue();
        }
    }

    private static void validateCurrency(ReservedBudget rb, Money itemAmount,
                                         LinkReservedBudgetSourceInput input) {
        if (!rb.getCurrency().equals(itemAmount.currency())) {
            throw new IllegalArgumentException(String.format(
                    "Currency mismatch: reserved budget currency=%s, %s '%s' currency=%s",
                    rb.getCurrency(), input.sourceType(), input.sourceId(), itemAmount.currency()));
        }
    }

    /**
     * Returns rb.getLinks() with the new link replacing any existing same-key entry.
     * Does NOT call rb.addLink() — we need the speculative list before mutating.
     *
     * @implNote Time complexity: O(L) where L = current link count.
     */
    private static List<ReservedBudgetLink> buildSpeculativeLinks(ReservedBudget rb,
                                                                   ReservedBudgetLink newLink) {
        List<ReservedBudgetLink> result = rb.getLinks().stream()
                .filter(l -> !(l.sourceType() == newLink.sourceType()
                        && l.sourceId().equals(newLink.sourceId())))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        result.add(newLink);
        return result;
    }
}
