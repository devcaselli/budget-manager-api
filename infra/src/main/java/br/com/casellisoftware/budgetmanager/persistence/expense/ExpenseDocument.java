package br.com.casellisoftware.budgetmanager.persistence.expense;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ExpenseDocument {

    @Id
    private String id;
    private String name;
    private BigDecimal cost;
    private Instant purchaseDate;
    private BigDecimal remaining;
    private String walletId;
}
