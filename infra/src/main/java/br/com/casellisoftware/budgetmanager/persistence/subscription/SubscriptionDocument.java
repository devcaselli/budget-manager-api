package br.com.casellisoftware.budgetmanager.persistence.subscription;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.YearMonth;
import java.util.List;

@Document("subscription")
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@CompoundIndex(name = "subscription_owner_active_window_idx", def = "{'ownerId': 1, 'startMonth': 1, 'endMonth': 1, 'state': 1}")
@CompoundIndex(name = "subscription_owner_creditcard_idx", def = "{'ownerId': 1, 'creditCardId': 1}")
public class SubscriptionDocument {

    @Id
    private String id;

    private String ownerId;

    @Version
    private Long version;

    private String description;
    private String currency;
    private String state;
    private FlagEnum flag;

    @Indexed
    private YearMonth startMonth;

    @Indexed(sparse = true)
    private YearMonth endMonth;

    private List<SubscriptionVersionDocument> versions;

    private String creditCardId;

    public SubscriptionDocument(String id,
                                Long version,
                                String description,
                                String currency,
                                String state,
                                FlagEnum flag,
                                YearMonth startMonth,
                                YearMonth endMonth,
                                List<SubscriptionVersionDocument> versions) {
        this(id, br.com.casellisoftware.budgetmanager.domain.subscription.Subscription.LEGACY_OWNER_ID, version,
                description, currency, state, flag, startMonth, endMonth, versions, null);
    }
}
