package br.com.casellisoftware.budgetmanager.rest.expense.dtos;

import java.util.List;

/**
 * HTTP response DTO for paginated expense results.
 */
public record PagedExpenseResponseDto(
        List<ExpenseResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
