package br.com.casellisoftware.budgetmanager.application.auth.boundary;

public interface AuthenticateUserBoundary {

    TokenOutput execute(AuthInput input);
}
