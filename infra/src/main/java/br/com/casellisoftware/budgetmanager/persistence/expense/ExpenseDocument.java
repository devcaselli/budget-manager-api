package br.com.casellisoftware.budgetmanager.persistence.expense;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Document
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseDocument {

    @Id
    private String id;

    @Version
    private Long version;

    private String name;
    private BigDecimal cost;
    private BigDecimal remaining;
    private String currency;
    private LocalDate purchaseDate;
    @Indexed
    private String walletId;
    private List<String> paymentIds;

}
