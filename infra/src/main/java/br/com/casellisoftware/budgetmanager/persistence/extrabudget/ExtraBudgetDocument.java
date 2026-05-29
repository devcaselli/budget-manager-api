package br.com.casellisoftware.budgetmanager.persistence.extrabudget;

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
import java.time.LocalDateTime;
import java.util.List;

@Document
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@CompoundIndexes({
        @CompoundIndex(name = "extra_owner_wallet_deleted_idx",
                       def = "{'ownerId':1,'walletId':1,'deleted':1}"),
        @CompoundIndex(name = "extra_owner_bullet_deleted_idx",
                       def = "{'ownerId':1,'allocations.bulletId':1,'deleted':1}")
})
public class ExtraBudgetDocument {

    @Id
    private String id;

    private String ownerId;

    @Version
    private Long version;

    private String description;
    private String walletId;
    private BigDecimal amount;
    private String currency;
    private List<AllocationSubDocument> allocations;
    private boolean deleted;
    private LocalDateTime deletedAt;
}
