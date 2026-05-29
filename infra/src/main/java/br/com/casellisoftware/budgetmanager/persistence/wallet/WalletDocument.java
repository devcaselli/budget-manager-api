package br.com.casellisoftware.budgetmanager.persistence.wallet;

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
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Getter
@Setter
@Document
@CompoundIndexes({
        @CompoundIndex(
                name = "uniq_active_production_per_month",
                def = "{'ownerId': 1, 'effectiveMonth': 1, 'state': 1, 'isClosed': 1}",
                unique = true,
                partialFilter = "{ 'state': 'PRODUCTION', 'isClosed': false }"
        ),
        @CompoundIndex(
                name = "wallet_owner_effective_month_idx",
                def = "{'ownerId': 1, 'effectiveMonth': 1}"
        )
})
public class WalletDocument {

    @Id
    private String id;

    private String ownerId;

    @Version
    private Long version;

    private String description;
    private BigDecimal budgetAmount;
    private String budgetCurrency;
    private BigDecimal remainingAmount;
    private String remainingCurrency;
    private LocalDate startDate;
    private LocalDate closedDate;
    private Boolean isClosed;
    private YearMonth effectiveMonth;
    private String state;
    private FlagEnum flag;

    public WalletDocument(String id,
                          Long version,
                          String description,
                          BigDecimal budgetAmount,
                          String budgetCurrency,
                          BigDecimal remainingAmount,
                          String remainingCurrency,
                          LocalDate startDate,
                          LocalDate closedDate,
                          Boolean isClosed,
                          YearMonth effectiveMonth,
                          String state) {
        this(id, br.com.casellisoftware.budgetmanager.domain.wallet.Wallet.LEGACY_OWNER_ID, version, description, budgetAmount, budgetCurrency, remainingAmount, remainingCurrency, startDate, closedDate, isClosed, effectiveMonth, state, FlagEnum.NONE);
    }

    public WalletDocument(String id,
                          Long version,
                          String description,
                          BigDecimal budgetAmount,
                          String budgetCurrency,
                          BigDecimal remainingAmount,
                          String remainingCurrency,
                          LocalDate startDate,
                          LocalDate closedDate,
                          Boolean isClosed,
                          YearMonth effectiveMonth,
                          String state,
                          FlagEnum flag) {
        this(id, br.com.casellisoftware.budgetmanager.domain.wallet.Wallet.LEGACY_OWNER_ID, version, description, budgetAmount, budgetCurrency, remainingAmount, remainingCurrency, startDate, closedDate, isClosed, effectiveMonth, state, flag);
    }
}
