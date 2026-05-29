package br.com.casellisoftware.budgetmanager.rest.creditcard.dtos;

import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import br.com.casellisoftware.budgetmanager.rest.installment.dtos.InstallmentResponseDto;
import br.com.casellisoftware.budgetmanager.rest.subscriptioncharge.dtos.SubscriptionChargeResponseDto;

import java.math.BigDecimal;
import java.util.List;

public record CreditCardChargesResponseDto(
        List<ExpenseResponseDto> expenses,
        List<InstallmentResponseDto> installments,
        List<SubscriptionChargeResponseDto> subscriptions,
        BigDecimal totalCost
) {
}
