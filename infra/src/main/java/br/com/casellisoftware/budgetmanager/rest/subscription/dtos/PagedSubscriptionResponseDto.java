package br.com.casellisoftware.budgetmanager.rest.subscription.dtos;

import java.util.List;

public record PagedSubscriptionResponseDto(
        List<SubscriptionResponseDto> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
}
