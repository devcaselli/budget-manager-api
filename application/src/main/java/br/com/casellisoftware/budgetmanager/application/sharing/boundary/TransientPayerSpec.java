package br.com.casellisoftware.budgetmanager.application.sharing.boundary;

import java.time.LocalDate;

public record TransientPayerSpec(
        String name,
        LocalDate paymentDate
) {
}
