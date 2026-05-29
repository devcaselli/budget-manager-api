package br.com.casellisoftware.budgetmanager.rest.payment.dtos;

import java.util.List;

public record PagedPaymentResponseDto(
        List<PaymentResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
