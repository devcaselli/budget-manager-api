package br.com.casellisoftware.budgetmanager.rest.creditcard.dtos;

import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;

import java.math.BigDecimal;
import java.util.List;

public record CreditCardExpensesResponseDto(
        List<ExpenseResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        BigDecimal totalCost
) {
}
