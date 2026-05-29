package br.com.casellisoftware.budgetmanager.rest.sharing.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TransientPayerRequestDto(
        @NotBlank @Size(max = 120) String name,
        LocalDate paymentDate
) {
}
