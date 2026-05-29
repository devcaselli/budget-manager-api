package br.com.casellisoftware.budgetmanager.domain.user;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public final class User {

    private final String id;
    private final String email;
    private final String passwordHash;
    private final LocalDateTime createdAt;

    public User(String id, String email, String passwordHash, LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.email = requireNonBlank(email, "email");
        this.passwordHash = requireNonBlank(passwordHash, "passwordHash");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    public static User create(String email, String passwordHash) {
        requireNonBlank(email, "email");
        requireNonBlank(passwordHash, "passwordHash");
        return new User(UUID.randomUUID().toString(), email, passwordHash, LocalDateTime.now());
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User other)) return false;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
