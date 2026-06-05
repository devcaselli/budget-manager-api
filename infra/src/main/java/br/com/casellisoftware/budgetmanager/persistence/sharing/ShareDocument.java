package br.com.casellisoftware.budgetmanager.persistence.sharing;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

@Document("share")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@CompoundIndexes({
        @CompoundIndex(name = "share_owner_idx", def = "{'ownerId': 1}"),
        @CompoundIndex(
                name = "share_source_idx",
                def = "{'sourceType': 1, 'sourceId': 1, 'status': 1}",
                unique = true,
                partialFilter = "{ 'status': 'ACTIVE' }"
        ),
        @CompoundIndex(name = "share_payer_idx", def = "{'quotas.payerId': 1, 'status': 1}")
})
public class ShareDocument {

    @Id
    private String id;

    private String ownerId;
    private String walletId;
    private String sourceType;
    private String sourceId;
    private BigDecimal totalAmount;
    private String totalCurrency;
    private BigDecimal ownerShare;
    private String ownerCurrency;
    private BigDecimal ownerRatio;
    private List<ShareQuotaDocument> quotas;
    private String status;
    private List<String> paymentIds;
    private Instant createdAt;
    private Instant revertedAt;
    private YearMonth stoppedFromMonth;

    @Version
    private Long version;
}
