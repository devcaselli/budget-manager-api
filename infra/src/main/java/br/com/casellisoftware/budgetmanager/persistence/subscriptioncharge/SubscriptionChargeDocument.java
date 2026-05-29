package br.com.casellisoftware.budgetmanager.persistence.subscriptioncharge;

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
import java.time.YearMonth;

@Document("subscription_charge")
@CompoundIndexes({
        @CompoundIndex(
                name = "uniq_subscription_wallet_month",
                def = "{'ownerId': 1, 'subscriptionId': 1, 'walletId': 1, 'month': 1}",
                unique = true
        ),
        @CompoundIndex(
                name = "uniq_charge_wallet_sub_month",
                def = "{'ownerId': 1, 'walletId': 1, 'subscriptionId': 1, 'month': 1}",
                unique = true
        )
})
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
public class SubscriptionChargeDocument {

    @Id
    private String id;

    private String ownerId;

    @Version
    private Long version;

    @Indexed
    private String subscriptionId;

    @Indexed
    private String walletId;

    @Indexed
    private YearMonth month;

    private BigDecimal amount;
    private BigDecimal remaining;
    private String currency;
    private FlagEnum flag;

    public SubscriptionChargeDocument(String id,
                                      Long version,
                                      String subscriptionId,
                                      String walletId,
                                      YearMonth month,
                                      BigDecimal amount,
                                      BigDecimal remaining,
                                      String currency) {
        this(id, br.com.casellisoftware.budgetmanager.domain.subscriptioncharge.SubscriptionCharge.LEGACY_OWNER_ID, version, subscriptionId, walletId, month, amount, remaining, currency, FlagEnum.NONE);
    }

    public SubscriptionChargeDocument(String id,
                                      Long version,
                                      String subscriptionId,
                                      String walletId,
                                      YearMonth month,
                                      BigDecimal amount,
                                      BigDecimal remaining,
                                      String currency,
                                      FlagEnum flag) {
        this(id, br.com.casellisoftware.budgetmanager.domain.subscriptioncharge.SubscriptionCharge.LEGACY_OWNER_ID, version, subscriptionId, walletId, month, amount, remaining, currency, flag);
    }
}
