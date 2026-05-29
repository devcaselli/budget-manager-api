package br.com.casellisoftware.budgetmanager.application.auth.boundary;

public record AuthInput(String email, String rawPassword) {
}
