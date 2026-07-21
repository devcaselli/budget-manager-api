package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.LinkedItemAmounts;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLink;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Computes how much of a {@link ReservedBudget}'s ceiling is consumed by its linked
 * subscriptions/installments in a given month, and how much remains.
 *
 * <p>Consumption is the sum of the post-share effective amount of every link that is
 * {@link ReservedBudgetLink#isApplicable(YearMonth) applicable} for the month and whose
 * source item is active that month. The amount is computed via the shared
 * {@link LinkedItemAmountsFactory}, guaranteeing the numbers match the cap validator and the
 * wallet calculators exactly (no drift).</p>
 *
 * <p>DB access strategy (no N+1): one batch call per type for items and one per type for
 * shares — ≤ 4 queries regardless of link count.</p>
 *
 * @implNote Time complexity: O(L × V) where L = links, V = max version history per item.
 *     Space: O(L).
 */
public class ReservedBudgetConsumptionQuery {

    private final SubscriptionRepository subscriptionRepository;
    private final InstallmentRepository installmentRepository;
    private final ShareRepository shareRepository;

    public ReservedBudgetConsumptionQuery(SubscriptionRepository subscriptionRepository,
                                          InstallmentRepository installmentRepository,
                                          ShareRepository shareRepository) {
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
        this.installmentRepository = Objects.requireNonNull(installmentRepository);
        this.shareRepository = Objects.requireNonNull(shareRepository);
    }

    /**
     * Returns the consumed/remaining split for {@code rb} in {@code month}.
     *
     * <p>If the RB has no links (or none applicable in {@code month}), consumed is zero and
     * remaining equals the ceiling.</p>
     *
     * @param rb      the reserved budget (never {@code null})
     * @param month   the month to evaluate (never {@code null})
     * @param ownerId owner scope for repository queries (never {@code null})
     */
    public ReservedBudgetConsumption consume(ReservedBudget rb, YearMonth month, String ownerId) {
        Objects.requireNonNull(rb, "rb must not be null");
        Objects.requireNonNull(month, "month must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");

        Money zero = Money.of(BigDecimal.ZERO, rb.getCurrency());

        // RB not yet active in this month (month < startMonth): no ceiling applies, nothing
        // is consumed. resolveAmount would throw for a pre-start month, so guard first.
        if (!rb.isApplicable(month)) {
            return new ReservedBudgetConsumption(zero, zero);
        }

        Money ceiling = rb.resolveAmount(month);

        List<ReservedBudgetLink> links = rb.getLinks();
        if (links.isEmpty()) {
            return new ReservedBudgetConsumption(zero, ceiling);
        }

        List<String> subIds = filterIds(links, ReservedBudgetLinkSourceType.SUBSCRIPTION);
        List<String> instIds = filterIds(links, ReservedBudgetLinkSourceType.INSTALLMENT);

        Map<String, Subscription> subscriptions = subIds.isEmpty()
                ? Map.of()
                : subscriptionRepository.findAllByIds(subIds, ownerId);
        Map<String, Installment> installments = instIds.isEmpty()
                ? Map.of()
                : installmentRepository.findAllByIds(instIds, ownerId);
        Map<String, Share> subShares = subIds.isEmpty()
                ? Map.of()
                : shareRepository.findActiveBySourceIds(ShareSourceType.SUBSCRIPTION, subIds, ownerId);
        Map<String, Share> instShares = instIds.isEmpty()
                ? Map.of()
                : shareRepository.findActiveBySourceIds(ShareSourceType.INSTALLMENT, instIds, ownerId);

        LinkedItemAmounts amounts = LinkedItemAmountsFactory.from(subscriptions, installments, subShares, instShares);

        Money consumed = zero;
        for (ReservedBudgetLink link : links) {
            if (!link.isApplicable(month)) {
                continue;
            }
            consumed = amounts.effectiveAmount(link, month)
                    .map(consumed::add)
                    .orElse(consumed);
        }

        return new ReservedBudgetConsumption(consumed, ceiling.subtract(consumed));
    }

    private static List<String> filterIds(List<ReservedBudgetLink> links, ReservedBudgetLinkSourceType type) {
        return links.stream()
                .filter(l -> l.sourceType() == type)
                .map(ReservedBudgetLink::sourceId)
                .toList();
    }

    /**
     * Consumed/remaining split for a reserved budget in a specific month.
     */
    public record ReservedBudgetConsumption(Money consumed, Money remaining) {
        public ReservedBudgetConsumption {
            Objects.requireNonNull(consumed, "consumed must not be null");
            Objects.requireNonNull(remaining, "remaining must not be null");
        }
    }
}
