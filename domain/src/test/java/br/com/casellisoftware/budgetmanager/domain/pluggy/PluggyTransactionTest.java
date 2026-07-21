package br.com.casellisoftware.budgetmanager.domain.pluggy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PluggyTransactionTest {

    private static final LocalDate DATE = LocalDate.of(2026, 5, 10);

    @Test
    void isExpense_creditCardPurchase_positiveAmount_returnsTrue() {
        // Confirmed against REAL production Pluggy data: a credit-card purchase (spend)
        // arrives with a POSITIVE amount (e.g. Netflix +72.80, Uber +17.95).
        PluggyTransaction tx = new PluggyTransaction("tx-1", "acc-1", "Netflix",
                BigDecimal.valueOf(72.80), "BRL", DATE, "CREDIT", "Entertainment", "CREDIT");

        assertThat(tx.isExpense()).isTrue();
    }

    @Test
    void isExpense_creditCardBillPayment_negativeAmount_returnsFalse() {
        // A bill payment / refund on a CREDIT account arrives NEGATIVE (e.g.
        // "PAG BOLETO BANCARIO" -4211) and must not be treated as an expense.
        PluggyTransaction tx = new PluggyTransaction("tx-2", "acc-1", "PAG BOLETO BANCARIO",
                BigDecimal.valueOf(-4211), "BRL", DATE, "DEBIT", null, "CREDIT");

        assertThat(tx.isExpense()).isFalse();
    }

    @Test
    void isExpense_creditCardPaymentCategory_anySign_returnsFalse() {
        // "Credit card payment" is a bill payment / transfer-out, not a purchase; excluding
        // it prevents double-counting card purchases that also arrive from the card account.
        // This guard applies regardless of sign or account type.
        PluggyTransaction tx = new PluggyTransaction("tx-3", "acc-1", "Payment received",
                BigDecimal.valueOf(-1253), "BRL", DATE, "DEBIT", "Credit card payment", "CREDIT");

        assertThat(tx.isExpense()).isFalse();
    }

    @Test
    void isExpense_creditCardPaymentCategory_positiveAmount_returnsFalse() {
        PluggyTransaction tx = new PluggyTransaction("tx-4", "acc-1", "Payment received",
                BigDecimal.valueOf(1253), "BRL", DATE, "CREDIT", "CREDIT CARD PAYMENT", "CREDIT");

        assertThat(tx.isExpense()).isFalse();
    }

    @Test
    void isExpense_bankSpend_negativeAmount_returnsTrue() {
        PluggyTransaction tx = new PluggyTransaction("tx-5", "acc-1", "Uber",
                BigDecimal.valueOf(-120), "BRL", DATE, "DEBIT", null, "BANK");

        assertThat(tx.isExpense()).isTrue();
    }

    @Test
    void isExpense_bankIncome_positiveAmount_returnsFalse() {
        PluggyTransaction tx = new PluggyTransaction("tx-6", "acc-1", "Salary",
                BigDecimal.valueOf(8500), "BRL", DATE, "CREDIT", null, "BANK");

        assertThat(tx.isExpense()).isFalse();
    }

    @Test
    void isExpense_nullAccountType_treatedAsBank_negativeAmountIsExpense() {
        // Defensive default: if accountType could not be resolved, fall back to the
        // conventional (BANK-like) sign rule rather than the inverted CREDIT rule.
        PluggyTransaction tx = new PluggyTransaction("tx-7", "acc-1", "Uber",
                BigDecimal.valueOf(-45.90), "BRL", DATE, "DEBIT", null, null);

        assertThat(tx.isExpense()).isTrue();
    }

    @Test
    void absAmount_returnsAbsoluteValue() {
        PluggyTransaction tx = new PluggyTransaction("tx-8", "acc-1", "Uber",
                BigDecimal.valueOf(-45.90), "BRL", DATE, "DEBIT", null, "BANK");

        assertThat(tx.absAmount()).isEqualByComparingTo(BigDecimal.valueOf(45.90));
    }

    @Test
    void withAccountType_returnsCopyWithNewAccountType() {
        PluggyTransaction tx = new PluggyTransaction("tx-9", "acc-1", "Netflix",
                BigDecimal.valueOf(72.80), "BRL", DATE, "CREDIT", "Entertainment", null);

        PluggyTransaction withType = tx.withAccountType("CREDIT");

        assertThat(withType.accountType()).isEqualTo("CREDIT");
        assertThat(withType.isExpense()).isTrue();
        assertThat(tx.accountType()).isNull();
    }
}
