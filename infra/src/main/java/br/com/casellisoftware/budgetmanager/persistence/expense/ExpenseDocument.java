package br.com.casellisoftware.budgetmanager.persistence.expense;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDate;

@Document
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExpenseDocument {

    @Id
    private String id;
    private String name;
    private BigDecimal cost;
    private BigDecimal remaining;
    private String currency;
    private LocalDate purchaseDate;
    private String walletId;
}
