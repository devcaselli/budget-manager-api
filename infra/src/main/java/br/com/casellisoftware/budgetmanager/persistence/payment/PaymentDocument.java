package br.com.casellisoftware.budgetmanager.persistence.payment;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentKind;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@CompoundIndexes({
        @CompoundIndex(name = "payment_owner_wallet_idx", def = "{'ownerId': 1, 'walletId': 1}"),
        @CompoundIndex(name = "payment_owner_expense_idx", def = "{'ownerId': 1, 'expenseId': 1}"),
        @CompoundIndex(name = "payment_owner_bullet_idx", def = "{'ownerId': 1, 'bulletId': 1}"),
        @CompoundIndex(name = "payment_owner_kind_idx", def = "{'ownerId': 1, 'kind': 1}")
})
public class PaymentDocument {

    @Id
    private String id;

    private String ownerId;

    private BigDecimal amount;
    private String currency;
    private Instant paymentDate;
    private String details;
    @Indexed
    private String expenseId;
    @Indexed
    private String walletId;
    @Indexed
    private String bulletId;
    private FlagEnum flag;
    private PaymentKind kind;
    @Indexed(sparse = true)
    private String payerId;
    @Indexed(sparse = true)
    private String shareId;
    private boolean reversal;
    @Indexed(sparse = true)
    private String reversedPaymentId;

    public PaymentDocument(String id,
                           BigDecimal amount,
                           String currency,
                           Instant paymentDate,
                           String details,
                           String expenseId,
                           String walletId,
                           String bulletId) {
        this(
                id,
                br.com.casellisoftware.budgetmanager.domain.payment.Payment.LEGACY_OWNER_ID,
                amount,
                currency,
                paymentDate,
                details,
                expenseId,
                walletId,
                bulletId,
                FlagEnum.NONE,
                PaymentKind.NORMAL,
                null,
                null,
                false,
                null
        );
    }

    public PaymentDocument(String id,
                           BigDecimal amount,
                           String currency,
                           Instant paymentDate,
                           String details,
                           String expenseId,
                           String walletId,
                           String bulletId,
                           FlagEnum flag) {
        this(
                id,
                br.com.casellisoftware.budgetmanager.domain.payment.Payment.LEGACY_OWNER_ID,
                amount,
                currency,
                paymentDate,
                details,
                expenseId,
                walletId,
                bulletId,
                flag,
                PaymentKind.NORMAL,
                null,
                null,
                false,
                null
        );
    }
}
