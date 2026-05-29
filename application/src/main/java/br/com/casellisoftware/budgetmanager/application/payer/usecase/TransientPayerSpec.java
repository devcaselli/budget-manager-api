package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import java.time.LocalDate;

public record TransientPayerSpec(
        String payerId,
        String name,
        LocalDate paymentDate
) {
}
