package br.com.casellisoftware.budgetmanager.persistence.expense;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Document
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@CompoundIndexes({
        @CompoundIndex(name = "expense_owner_wallet_idx", def = "{'ownerId': 1, 'walletId': 1}"),
        @CompoundIndex(name = "expense_owner_credit_card_idx", def = "{'ownerId': 1, 'creditCardId': 1}"),
        @CompoundIndex(name = "expense_owner_installment_idx", def = "{'ownerId': 1, 'installmentId': 1}"),
        @CompoundIndex(name = "expense_owner_purchase_date_idx", def = "{'ownerId': 1, 'purchaseDate': 1}"),
        // Partial unique index for deduplication: only enforced when sourcePendingId is present.
        // NOTE: sparse=true on a compound index does not exclude null-field documents when the
        // other indexed field (ownerId) is always present. Use a programmatic partial index
        // (partialFilterExpression) instead — see MongoIndexConfiguration for the DDL.
        // This annotation entry is intentionally left as a non-unique non-sparse index so that
        // the auto-index-creation path does not create a conflicting unique constraint.
        @CompoundIndex(
                name = "expense_owner_source_pending_idx",
                def = "{'ownerId': 1, 'sourcePendingId': 1}"
        )
})
public class ExpenseDocument {

    @Id
    private String id;

    private String ownerId;

    @Version
    private Long version;

    private String name;
    private BigDecimal cost;
    private BigDecimal remaining;
    private String currency;
    private LocalDate purchaseDate;
    @Indexed
    private String walletId;
    @Indexed
    private String creditCardId;
    @Indexed
    private String installmentId;
    private List<String> paymentIds;
    private FlagEnum flag;
    private boolean hidden;

    /** Optional id from ingest-api used for deduplication on sync. Null for manually created expenses. */
    private String sourcePendingId;

    public ExpenseDocument(String id,
                           Long version,
                           String name,
                           BigDecimal cost,
                           BigDecimal remaining,
                           String currency,
                           LocalDate purchaseDate,
                           String walletId,
                           String creditCardId,
                           List<String> paymentIds,
                           FlagEnum flag) {
        this(id, br.com.casellisoftware.budgetmanager.domain.expense.Expense.LEGACY_OWNER_ID, version, name, cost, remaining, currency, purchaseDate, walletId, creditCardId, null, paymentIds, flag, false, null);
    }

    public ExpenseDocument(String id,
                           Long version,
                           String name,
                           BigDecimal cost,
                           BigDecimal remaining,
                           String currency,
                           LocalDate purchaseDate,
                           String walletId,
                           String creditCardId,
                           String installmentId,
                           List<String> paymentIds,
                           FlagEnum flag,
                           boolean hidden) {
        this(id, br.com.casellisoftware.budgetmanager.domain.expense.Expense.LEGACY_OWNER_ID, version, name, cost, remaining, currency, purchaseDate, walletId, creditCardId, installmentId, paymentIds, flag, hidden, null);
    }
}
