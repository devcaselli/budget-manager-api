package br.com.casellisoftware.budgetmanager.rest.creditcard.dtos;

import java.util.List;

public record PagedCreditCardResponseDto(
        List<CreditCardResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
