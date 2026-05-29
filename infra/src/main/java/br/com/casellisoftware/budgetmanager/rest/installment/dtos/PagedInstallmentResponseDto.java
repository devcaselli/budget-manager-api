package br.com.casellisoftware.budgetmanager.rest.installment.dtos;

import java.util.List;

/**
 * HTTP response DTO for paginated installment results.
 */
public record PagedInstallmentResponseDto(
        List<InstallmentResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
