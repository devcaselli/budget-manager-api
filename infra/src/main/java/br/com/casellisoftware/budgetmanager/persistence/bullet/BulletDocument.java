package br.com.casellisoftware.budgetmanager.persistence.bullet;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

@Document
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BulletDocument {

    @Id
    private String id;

    @Version
    private Long version;

    private String description;
    private BigDecimal budget;
    private BigDecimal remaining;
    private String currency;
    @Indexed
    private String walletId;

}
