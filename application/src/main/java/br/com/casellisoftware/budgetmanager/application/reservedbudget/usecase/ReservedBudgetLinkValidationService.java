package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.EffectiveShareAmount;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentAffectsWalletSpecification;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.LinkedItemAmounts;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudget;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLink;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkCapValidator;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionRepository;

import java.time.YearMonth;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Application service that builds a {@link LinkedItemAmounts} implementation from
 * the repository layer and delegates cap validation to {@link ReservedBudgetLinkCapValidator}.
 *
 * <p>This service is the single place where the post-share amount calculation meets
 * the domain validator. It must produce numbers identical to what the wallet calculators
 * produce — correctness is guaranteed by delegating to {@link EffectiveShareAmount}.</p>
 *
 * <p>DB access strategy (no N+1):</p>
 * <ul>
 *   <li>One batch call per type (subscription, installment) to load items by id.</li>
 *   <li>One batch call per type to load active shares.</li>
 *   <li>Total: ≤ 4 queries regardless of number of links.</li>
 * </ul>
 *
 * @implNote Time complexity: O(L × V) where L = links, V = max version history per item.
 *     Space: O(L).
 */
public class ReservedBudgetLinkValidationService {

    private final SubscriptionRepository subscriptionRepository;
    private final InstallmentRepository installmentRepository;
    private final ShareRepository shareRepository;
    private final ReservedBudgetLinkCapValidator capValidator;

    public ReservedBudgetLinkValidationService(SubscriptionRepository subscriptionRepository,
                                               InstallmentRepository installmentRepository,
                                               ShareRepository shareRepository,
                                               ReservedBudgetLinkCapValidator capValidator) {
        this.subscriptionRepository = Objects.requireNonNull(subscriptionRepository);
        this.installmentRepository = Objects.requireNonNull(installmentRepository);
        this.shareRepository = Objects.requireNonNull(shareRepository);
        this.capValidator = Objects.requireNonNull(capValidator);
    }

    /**
     * Validates that {@code effectiveLinks} do not exceed the cap of {@code rb} in any month.
     *
     * <p>Fetches items and shares in batch, then delegates to the domain validator. Throws
     * {@link br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkCapExceededException}
     * if the cap is violated.</p>
     *
     * @param rb             the reserved budget whose ceiling is the constraint
     * @param effectiveLinks the links to validate (may be a speculative set that includes
     *                       a new link not yet persisted)
     * @param ownerId        owner scope for repository queries
     */
    public void validate(ReservedBudget rb, List<ReservedBudgetLink> effectiveLinks, String ownerId) {
        Objects.requireNonNull(rb, "rb must not be null");
        Objects.requireNonNull(effectiveLinks, "effectiveLinks must not be null");
        Objects.requireNonNull(ownerId, "ownerId must not be null");

        if (effectiveLinks.isEmpty()) {
            return;
        }

        List<String> subIds = filterIds(effectiveLinks, ReservedBudgetLinkSourceType.SUBSCRIPTION);
        List<String> instIds = filterIds(effectiveLinks, ReservedBudgetLinkSourceType.INSTALLMENT);

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

        LinkedItemAmounts amounts = buildLinkedItemAmounts(subscriptions, installments, subShares, instShares);
        capValidator.validate(rb, effectiveLinks, amounts);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private LinkedItemAmounts buildLinkedItemAmounts(Map<String, Subscription> subscriptions,
                                                     Map<String, Installment> installments,
                                                     Map<String, Share> subShares,
                                                     Map<String, Share> instShares) {
        return new LinkedItemAmounts() {

            @Override
            public Optional<Money> effectiveAmount(ReservedBudgetLink link, YearMonth month) {
                if (link.sourceType() == ReservedBudgetLinkSourceType.SUBSCRIPTION) {
                    Subscription sub = subscriptions.get(link.sourceId());
                    if (sub == null) return Optional.empty();
                    // Subscription applies only within [startMonth, endMonth)
                    if (month.isBefore(sub.getStartMonth())) return Optional.empty();
                    if (sub.getEndMonth() != null && !month.isBefore(sub.getEndMonth())) return Optional.empty();
                    Share share = subShares.get(link.sourceId());
                    return Optional.of(EffectiveShareAmount.forSubscription(sub, share, month));
                } else {
                    Installment inst = installments.get(link.sourceId());
                    if (inst == null) return Optional.empty();
                    // Installment applies only in its active window
                    if (!InstallmentAffectsWalletSpecification.isSatisfiedBy(inst, month)) return Optional.empty();
                    Share share = instShares.get(link.sourceId());
                    return Optional.of(EffectiveShareAmount.forInstallment(inst, share, month));
                }
            }

            @Override
            public Set<YearMonth> breakpoints(ReservedBudgetLink link) {
                Set<YearMonth> bps = new HashSet<>();
                bps.add(link.fromMonth());

                if (link.sourceType() == ReservedBudgetLinkSourceType.SUBSCRIPTION) {
                    Subscription sub = subscriptions.get(link.sourceId());
                    if (sub != null) {
                        // Sub version months: where sub amount steps
                        sub.getVersions().forEach(v -> bps.add(v.effectiveMonth()));
                        // End month: sub stops applying
                        if (sub.getEndMonth() != null) bps.add(sub.getEndMonth());
                    }
                    Share share = subShares.get(link.sourceId());
                    if (share != null && share.getStoppedFromMonth() != null) {
                        // Share stop month: effective amount changes (full amount from here)
                        bps.add(share.getStoppedFromMonth());
                    }
                } else {
                    Installment inst = installments.get(link.sourceId());
                    if (inst != null) {
                        // Installment first month of activity
                        bps.add(inst.getSourceEffectiveMonth());
                        // Month after last installment: inst expires → sum drops
                        bps.add(inst.getLastInstallmentDate().plusMonths(1));
                    }
                    Share share = instShares.get(link.sourceId());
                    if (share != null && share.getStoppedFromMonth() != null) {
                        bps.add(share.getStoppedFromMonth());
                    }
                }

                return bps;
            }
        };
    }

    private static List<String> filterIds(Collection<ReservedBudgetLink> links,
                                          ReservedBudgetLinkSourceType type) {
        return links.stream()
                .filter(l -> l.sourceType() == type)
                .map(ReservedBudgetLink::sourceId)
                .collect(Collectors.toList());
    }
}
