package br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos;

import java.util.List;

public record PagedReservedBudgetResponseDto(
        List<ReservedBudgetResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
