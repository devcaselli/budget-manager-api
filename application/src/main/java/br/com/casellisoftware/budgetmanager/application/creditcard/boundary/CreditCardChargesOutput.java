package br.com.casellisoftware.budgetmanager.application.creditcard.boundary;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.SubscriptionChargeOutput;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated view of everything that debits a credit card in a given month:
 *
 * <ul>
 *   <li>{@code expenses} — one-shot purchases ({@code installmentId == null},
 *   {@code hidden == false}). The hidden "source" expense produced by a
 *   standard installment is intentionally excluded.</li>
 *   <li>{@code installments} — active installments whose payment window
 *   covers the requested month and that are tied to the credit card.</li>
 *   <li>{@code subscriptions} — recurring subscription charges materialized
 *   (preview) for the month and tied to the credit card.</li>
 * </ul>
 *
 * <p>{@code totalCost} aggregates all three streams in the credit card's
 * currency.</p>
 */
public record CreditCardChargesOutput(
        List<ExpenseOutput> expenses,
        List<InstallmentOutput> installments,
        List<SubscriptionChargeOutput> subscriptions,
        BigDecimal totalCost
) {
}
