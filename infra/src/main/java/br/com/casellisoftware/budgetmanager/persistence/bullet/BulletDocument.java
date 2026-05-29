package br.com.casellisoftware.budgetmanager.persistence.bullet;

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

@Document
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@CompoundIndexes({
        @CompoundIndex(name = "bullet_owner_wallet_idx", def = "{'ownerId': 1, 'walletId': 1}")
})
public class BulletDocument {

    @Id
    private String id;

    private String ownerId;

    @Version
    private Long version;

    private String description;
    private BigDecimal budget;
    private BigDecimal remaining;
    private String currency;
    @Indexed
    private String walletId;
    private FlagEnum flag;

    public BulletDocument(String id,
                          Long version,
                          String description,
                          BigDecimal budget,
                          BigDecimal remaining,
                          String currency,
                          String walletId) {
        this(id, br.com.casellisoftware.budgetmanager.domain.bullet.Bullet.LEGACY_OWNER_ID, version, description, budget, remaining, currency, walletId, FlagEnum.NONE);
    }

    public BulletDocument(String id,
                          Long version,
                          String description,
                          BigDecimal budget,
                          BigDecimal remaining,
                          String currency,
                          String walletId,
                          FlagEnum flag) {
        this(id, br.com.casellisoftware.budgetmanager.domain.bullet.Bullet.LEGACY_OWNER_ID, version, description, budget, remaining, currency, walletId, flag);
    }
}
