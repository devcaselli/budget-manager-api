package br.com.casellisoftware.budgetmanager.application.auth.boundary;

public interface TokenGeneratorPort {

    TokenOutput generate(String userId, String email);
}
