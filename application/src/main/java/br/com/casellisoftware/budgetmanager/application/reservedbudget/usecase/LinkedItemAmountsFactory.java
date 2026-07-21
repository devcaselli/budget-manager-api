package br.com.casellisoftware.budgetmanager.application.reservedbudget.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.EffectiveShareAmount;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentAffectsWalletSpecification;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.LinkedItemAmounts;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLink;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetLinkSourceType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;

import java.time.YearMonth;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Builds a {@link LinkedItemAmounts} from already-loaded item and share maps.
 *
 * <p>Single source of truth for the post-share effective amount of a linked item in a given
 * month. Shared by {@link ReservedBudgetLinkValidationService} (cap validation) and
 * {@link ReservedBudgetConsumptionQuery} (consumed/remaining reporting) so the two paths can
 * never drift.</p>
 *
 * <p>The maps must be loaded in batch by the caller (one query per type) to avoid N+1.</p>
 */
final class LinkedItemAmountsFactory {

    private LinkedItemAmountsFactory() {
    }

    /**
     * Returns a {@link LinkedItemAmounts} view over the supplied maps.
     *
     * @param subscriptions subscriptions by id (post-batch load)
     * @param installments  installments by id (post-batch load)
     * @param subShares      active subscription shares by source id
     * @param instShares     active installment shares by source id
     */
    static LinkedItemAmounts from(Map<String, Subscription> subscriptions,
                                  Map<String, Installment> installments,
                                  Map<String, Share> subShares,
                                  Map<String, Share> instShares) {
        Objects.requireNonNull(subscriptions, "subscriptions must not be null");
        Objects.requireNonNull(installments, "installments must not be null");
        Objects.requireNonNull(subShares, "subShares must not be null");
        Objects.requireNonNull(instShares, "instShares must not be null");

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
}
