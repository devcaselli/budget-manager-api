package br.com.casellisoftware.budgetmanager.rest.auth.dtos;

import java.time.LocalDateTime;

public record UserResponseDto(String id, String email, LocalDateTime createdAt) {
}
