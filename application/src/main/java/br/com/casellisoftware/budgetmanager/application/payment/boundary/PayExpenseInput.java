package br.com.casellisoftware.budgetmanager.application.payment.boundary;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Input DTO for the pay-expense use case.
 *
 * <p>Carries everything needed to register a payment against an expense
 * and decrement its matching bullet. The REST layer is responsible for
 * validating each field — this record is a pure transport contract
 * between the HTTP adapter and the application layer.</p>
 */
public record PayExpenseInput(
        BigDecimal amount,
        Instant paymentDate,
        String details,
        String expenseId,
        String walletId,
        String bulletId
) {
}
