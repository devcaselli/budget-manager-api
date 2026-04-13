package br.com.casellisoftware.budgetmanager.persistence.payment;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;

@Document
@Getter
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentDocument {

    @Id
    private String id;
    private BigDecimal amount;
    private Instant paymentDate;
    private String details;
    private String expenseId;
    private String walletId;
}