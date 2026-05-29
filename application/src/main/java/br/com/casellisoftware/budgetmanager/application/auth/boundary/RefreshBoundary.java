package br.com.casellisoftware.budgetmanager.application.auth.boundary;

public interface RefreshBoundary {
    TokenOutput execute(RefreshTokenInput input);
}
