package br.com.casellisoftware.budgetmanager.rest.sync.dtos;

import jakarta.validation.constraints.NotNull;

public record SyncPreferenceRequestDto(@NotNull Boolean enabled) {
}
