package br.com.casellisoftware.budgetmanager.domain.pluggy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * A Pluggy {@code transaction} belonging to a {@link PluggyAccount}.
 *
 * <p>{@code amount} is the raw, signed value as returned by Pluggy. Its meaning depends on
 * the owning account's {@code accountType} — see {@link #isExpense()}.</p>
 *
 * <p><b>Direction rule (as of this revision — confirmed against REAL production Pluggy
 * data, not sandbox):</b> the sign convention INVERTS by account type:</p>
 * <ul>
 *   <li>{@code accountType == "CREDIT"} (credit card): a purchase/spend arrives with a
 *       POSITIVE {@code amount} (e.g. Netflix +72.80, Uber +17.95, "COMPRA PARCELADA"
 *       +102.80). A bill payment or refund arrives NEGATIVE (e.g. "PAG BOLETO BANCARIO"
 *       -4211, "Pagamento recebido" -1253) and must NOT be treated as an expense.</li>
 *   <li>Any other {@code accountType} (e.g. {@code "BANK"} / checking account): a spend is
 *       NEGATIVE; income (salary, transfers in) is POSITIVE — the conventional sign rule.</li>
 * </ul>
 *
 * <p>This replaces an earlier sign-based-only rule that was calibrated on the Pluggy
 * sandbox and confirmed BACKWARDS for real production credit-card connectors: the sandbox
 * had negative-amount card purchases, while real production credit cards send
 * positive-amount purchases and negative-amount settlements.</p>
 *
 * <p>The {@code category == "Credit card payment"} exception is kept as a belt-and-suspenders
 * guard (regardless of sign or account type): this is a real Pluggy Level-2 category under
 * "Transfers" representing a bill payment / transfer-out, never a purchase, and excluding it
 * prevents double-counting card purchases that already arrive individually from the card
 * account. It also covers bank-side "PAGAMENTO FATURA" lines, which are negative on a BANK
 * account and would already be excluded by the sign rule, but the explicit category guard
 * keeps the intent obvious regardless of which account surfaces the row.</p>
 *
 * <p>{@code type} ({@code DEBIT}/{@code CREDIT}, Pluggy's own directional field on the
 * transaction) is retained for display but is NOT used for direction — proven unreliable.</p>
 *
 * @param id          Pluggy transaction id (used as {@code sourcePendingId} for dedup)
 * @param accountId   id of the parent {@link PluggyAccount}
 * @param description transaction description/merchant, as returned by Pluggy
 * @param amount      raw signed amount; meaning depends on {@code accountType}, see class Javadoc
 * @param currency    ISO currency code (Pluggy field {@code currencyCode})
 * @param date        transaction date
 * @param type        Pluggy's transaction-level directional field ({@code "DEBIT"}/{@code
 *                    "CREDIT"}); may be {@code null}; NOT used by {@link #isExpense()}
 * @param category    Pluggy's transaction category display string (e.g. {@code "Credit card
 *                    payment"}); may be {@code null}
 * @param accountType the owning {@link PluggyAccount}'s {@code type} (e.g. {@code "BANK"},
 *                    {@code "CREDIT"}), resolved by the fetch use case via {@code
 *                    PluggyClient#listAccounts}; drives the sign convention in {@link
 *                    #isExpense()}
 */
public record PluggyTransaction(String id, String accountId, String description, BigDecimal amount,
                                 String currency, LocalDate date, String type, String category,
                                 String accountType) {

    private static final String CREDIT_CARD_PAYMENT_CATEGORY = "Credit card payment";
    private static final String CREDIT_ACCOUNT_TYPE = "CREDIT";

    public PluggyTransaction {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(accountId, "accountId must not be null");
        Objects.requireNonNull(amount, "amount must not be null");
    }

    /**
     * {@code true} when this transaction represents money leaving the account (an expense
     * candidate).
     *
     * <p>Rule: a {@code category} of {@code "Credit card payment"} is always non-expense
     * (bill payment / transfer-out). Otherwise the sign convention depends on {@code
     * accountType}: on a {@code CREDIT} account, a purchase is {@code amount > 0}; on any
     * other account type (bank/checking), a spend is {@code amount < 0}. See class Javadoc
     * for the production-data rationale.</p>
     *
     * @implNote Time complexity: O(1), space complexity: O(1).
     */
    public boolean isExpense() {
        if (category != null && category.equalsIgnoreCase(CREDIT_CARD_PAYMENT_CATEGORY)) {
            return false;
        }
        if (CREDIT_ACCOUNT_TYPE.equalsIgnoreCase(accountType)) {
            return amount.signum() > 0;
        }
        return amount.signum() < 0;
    }

    /** Absolute value of {@link #amount}, suitable for {@code Money} construction. */
    public BigDecimal absAmount() {
        return amount.abs();
    }

    /**
     * Returns a copy of this transaction with {@code accountType} set to the given value.
     * Used by {@code FetchPluggyTransactionsForItemUseCase} to attach the owning account's
     * type (resolved via a separate {@code PluggyClient#listAccounts} call) after the vendor
     * adapter constructs the transaction without it.
     */
    public PluggyTransaction withAccountType(String accountType) {
        return new PluggyTransaction(id, accountId, description, amount, currency, date, type, category, accountType);
    }
}
