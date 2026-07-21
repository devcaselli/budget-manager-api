package br.com.casellisoftware.budgetmanager.rest.pluggy.dtos;

import jakarta.validation.constraints.NotBlank;

public record RegisterItemRequestDto(@NotBlank String itemId) {
}
