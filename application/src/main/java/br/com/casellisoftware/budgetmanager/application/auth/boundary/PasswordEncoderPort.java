package br.com.casellisoftware.budgetmanager.application.auth.boundary;

public interface PasswordEncoderPort {

    String encode(String rawPassword);

    boolean matches(String rawPassword, String encodedPassword);
}
