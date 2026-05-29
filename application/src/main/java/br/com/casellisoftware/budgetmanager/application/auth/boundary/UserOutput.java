package br.com.casellisoftware.budgetmanager.application.auth.boundary;

import java.time.LocalDateTime;

public record UserOutput(String id, String email, LocalDateTime createdAt) {
}
