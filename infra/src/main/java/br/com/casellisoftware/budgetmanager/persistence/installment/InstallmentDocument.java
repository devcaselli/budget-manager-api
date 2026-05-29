package br.com.casellisoftware.budgetmanager.persistence.installment;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

@Document
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@CompoundIndexes({
        @CompoundIndex(
                name = "installment_active_window_idx",
                def = "{'ownerId': 1, 'deleted': 1, 'sourceEffectiveMonth': 1, 'lastInstallmentDate': 1}"
        ),
        @CompoundIndex(
                name = "installment_owner_source_wallet_idx",
                def = "{'ownerId': 1, 'sourceWalletId': 1, 'deleted': 1}"
        )
})
public class InstallmentDocument {

    @Id
    private String id;

    private String ownerId;

    @Version
    private Long version;

    private String description;

    private String details;

    private BigDecimal originalAmount;
    private BigDecimal installmentAmount;
    private String currency;

    private Integer installmentNumber;
    private LocalDate purchaseDate;

    @Indexed
    private YearMonth lastInstallmentDate;

    @Indexed
    private String creditCardId;

    @Indexed
    private String sourceExpenseId;

    @Indexed
    private String sourceWalletId;

    @Indexed
    private YearMonth sourceEffectiveMonth;

    private boolean deleted;

    private LocalDateTime deletedAt;

    private FlagEnum flag;

    public InstallmentDocument(String id,
                               Long version,
                               String description,
                               BigDecimal originalAmount,
                               BigDecimal installmentAmount,
                               String currency,
                               Integer installmentNumber,
                               LocalDate purchaseDate,
                               YearMonth lastInstallmentDate,
                               String creditCardId,
                               String sourceExpenseId,
                               String sourceWalletId,
                               YearMonth sourceEffectiveMonth,
                               boolean deleted,
                               LocalDateTime deletedAt,
                               FlagEnum flag) {
        this(id, br.com.casellisoftware.budgetmanager.domain.installment.Installment.LEGACY_OWNER_ID, version, description, null,
                originalAmount, installmentAmount, currency, installmentNumber, purchaseDate, lastInstallmentDate,
                creditCardId, sourceExpenseId, sourceWalletId, sourceEffectiveMonth, deleted, deletedAt, flag);
    }
}
