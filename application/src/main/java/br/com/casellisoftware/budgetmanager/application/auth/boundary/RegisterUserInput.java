package br.com.casellisoftware.budgetmanager.application.auth.boundary;

public record RegisterUserInput(String email, String rawPassword) {
}
