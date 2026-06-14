package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.YearMonth;
import java.util.Optional;
import java.util.Set;

/**
 * Strategy interface that abstracts how the application layer provides effective monetary
 * amounts and breakpoint months for items linked to a {@link ReservedBudget}.
 *
 * <p>The domain service {@link ReservedBudgetLinkCapValidator} is entirely decoupled from
 * subscription/installment repositories; implementations in the application layer resolve
 * the data and inject them through this interface (Dependency Inversion Principle).</p>
 *
 * <p>Definitions:</p>
 * <ul>
 *   <li><b>effectiveAmount</b>: the post-share monetary value that a linked item would
 *       deduct from the wallet in a given month (i.e. {@code resolveAmount * ownerRatio}
 *       for subscriptions, or the installment face value for installments). Returns
 *       {@link Optional#empty()} if the item does not apply in that month (e.g. installment
 *       already expired, subscription not yet started, or before the link's fromMonth).</li>
 *   <li><b>breakpoints</b>: the set of {@link YearMonth} values at which the effective
 *       amount for this specific link may change (version month changes, share ratio changes,
 *       expiry month, etc.). The validator uses breakpoints to build the finite set of
 *       intervals that must be checked without iterating month by month.</li>
 * </ul>
 *
 * @implNote Time complexity of the cap validation is O(B × L) where B is the total number
 *     of breakpoints across all links and L is the number of links per breakpoint check.
 *     In practice B is bounded by the number of version + share entries — typically O(1).
 */
public interface LinkedItemAmounts {

    /**
     * Returns the effective post-share amount that the given link contributes to the
     * reserved budget ceiling in {@code month}, or {@link Optional#empty()} if the linked
     * item does not apply in that month.
     *
     * @param link  the link being evaluated (never {@code null})
     * @param month the month being evaluated (never {@code null})
     * @return the effective amount, or empty if the item is inactive in that month
     */
    Optional<Money> effectiveAmount(ReservedBudgetLink link, YearMonth month);

    /**
     * Returns the set of months at which the effective amount for the given link may change.
     * The validator will check exactly one representative month per interval delimited by
     * these breakpoints.
     *
     * <p>Must include at minimum: the link's {@code fromMonth} and the month after the last
     * expected change (e.g. the month after the last installment date). An empty set is
     * valid only if the item never applies (in which case {@link #effectiveAmount} always
     * returns empty).</p>
     *
     * @param link the link whose breakpoints are requested (never {@code null})
     * @return a non-null, possibly empty set of breakpoint months
     */
    Set<YearMonth> breakpoints(ReservedBudgetLink link);
}
