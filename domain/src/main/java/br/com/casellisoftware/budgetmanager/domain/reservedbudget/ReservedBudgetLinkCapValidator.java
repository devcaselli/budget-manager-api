package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeSet;

/**
 * Pure domain service that enforces the cap invariant for a {@link ReservedBudget}:
 *
 * <blockquote>For every month ≥ the earliest link's {@code fromMonth}, the sum of all
 * applicable linked item effective amounts must not exceed the reserved budget ceiling
 * resolved for that month.</blockquote>
 *
 * <h2>Algorithm — finite breakpoint scan</h2>
 * <p>Both the RB ceiling and every linked item's effective amount are <em>step functions</em>
 * of time: they remain constant between changes and only change at discrete months
 * (version effectiveMonth, subscription amount version, share ratio change, installment
 * expiry, link fromMonth, etc.). These are called <em>breakpoints</em>.</p>
 *
 * <p>Therefore, instead of iterating month by month into an unbounded future, we collect
 * all breakpoints ≥ {@code rb.getStartMonth()} into a {@link TreeSet} and check exactly
 * one representative month per interval (the breakpoint that opens each interval). The
 * last breakpoint represents the constant tail extending to infinity — if the cap holds
 * there it holds forever.</p>
 *
 * <p>Breakpoints come from two sources:</p>
 * <ol>
 *   <li>RB version months (where the ceiling steps).</li>
 *   <li>Per-link breakpoints provided by {@link LinkedItemAmounts#breakpoints(ReservedBudgetLink)}
 *       (where a linked item's effective amount steps).</li>
 * </ol>
 *
 * @implNote Time complexity: O(B × L) where B = total breakpoints across all links and
 *     L = number of links. In practice both are small and bounded by version history size.
 *     Space complexity: O(B).
 */
public class ReservedBudgetLinkCapValidator {

    /**
     * Validates that the sum of all applicable link amounts does not exceed the RB ceiling
     * in any month, using a breakpoint-based finite scan.
     *
     * @param rb            the reserved budget (never {@code null})
     * @param effectiveLinks the candidate set of links to validate against the ceiling —
     *                       may differ from {@code rb.getLinks()} when called speculatively
     *                       (e.g., "what if we add this new link?")
     * @param amounts        the strategy for resolving amounts and breakpoints per link
     * @throws ReservedBudgetLinkCapExceededException if the cap invariant is violated in
     *                                                any month
     * @throws NullPointerException                   if any argument is {@code null}
     */
    public void validate(ReservedBudget rb,
                         List<ReservedBudgetLink> effectiveLinks,
                         LinkedItemAmounts amounts) {
        Objects.requireNonNull(rb, "rb must not be null");
        Objects.requireNonNull(effectiveLinks, "effectiveLinks must not be null");
        Objects.requireNonNull(amounts, "amounts must not be null");

        if (effectiveLinks.isEmpty()) {
            return;
        }

        TreeSet<YearMonth> breakpoints = collectBreakpoints(rb, effectiveLinks, amounts);

        for (YearMonth month : breakpoints) {
            Money ceiling = rb.resolveAmount(month);
            Money sum = sumEffectiveAmounts(effectiveLinks, amounts, month, ceiling);

            if (sum.isGreaterThan(ceiling)) {
                throw new ReservedBudgetLinkCapExceededException(month, sum, ceiling);
            }
        }
    }

    /**
     * Collects the union of all breakpoints from the RB versions and from every link.
     * Only months ≥ {@code rb.getStartMonth()} are included since the RB does not apply
     * before that.
     *
     * @implNote Time complexity: O(V + B) where V = number of RB versions and B = total
     *     link breakpoints. Space: O(V + B).
     */
    private TreeSet<YearMonth> collectBreakpoints(ReservedBudget rb,
                                                  List<ReservedBudgetLink> effectiveLinks,
                                                  LinkedItemAmounts amounts) {
        TreeSet<YearMonth> breakpoints = new TreeSet<>();

        // RB version months define where the ceiling steps
        for (ReservedBudgetVersion version : rb.getVersions()) {
            breakpoints.add(version.effectiveMonth());
        }

        // Each link contributes its own breakpoints (sub versions, share changes, expiry...)
        for (ReservedBudgetLink link : effectiveLinks) {
            for (YearMonth bp : amounts.breakpoints(link)) {
                if (!bp.isBefore(rb.getStartMonth())) {
                    breakpoints.add(bp);
                }
            }
        }

        return breakpoints;
    }

    /**
     * Sums the effective amounts of all applicable links for {@code month}.
     * Uses Money.zero(ceiling.currency()) as the accumulator to guarantee currency
     * consistency — the use case must enforce item.currency == rb.currency upstream.
     *
     * @implNote Time complexity: O(L) where L = number of links.
     */
    private Money sumEffectiveAmounts(List<ReservedBudgetLink> effectiveLinks,
                                      LinkedItemAmounts amounts,
                                      YearMonth month,
                                      Money ceiling) {
        Money sum = Money.zero(ceiling.currency());
        for (ReservedBudgetLink link : effectiveLinks) {
            if (!link.isApplicable(month)) {
                continue;
            }
            Optional<Money> amount = amounts.effectiveAmount(link, month);
            if (amount.isPresent()) {
                sum = sum.add(amount.get());
            }
        }
        return sum;
    }
}
