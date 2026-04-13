package br.com.casellisoftware.budgetmanager.rest.payment.dtos;

public record PayRequestDto(

        PaymentRequestDto payment,
        String bulletId,
        String expenseId
) {
}
