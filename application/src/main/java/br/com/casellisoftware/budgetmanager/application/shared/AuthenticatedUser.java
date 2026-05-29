package br.com.casellisoftware.budgetmanager.application.shared;

import java.util.Objects;

public record AuthenticatedUser(String ownerId) {

    public static final String LEGACY_OWNER_ID = "legacy";

    public AuthenticatedUser {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        if (ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
    }
}
