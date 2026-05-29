package br.com.casellisoftware.budgetmanager.domain.creditcard;

import br.com.casellisoftware.budgetmanager.domain.shared.LabelNormalizer;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a credit card.
 *
 * <p>Immutable. Use {@link #create(String)} for new instances and the public
 * constructor for persistence reconstruction.</p>
 *
 * <p>{@code labels} are user-supplied strings (e.g. "Bradescard", "BRADSCO") used
 * to match SMS-extracted card identifiers during ingest sync. {@code normalizedLabels}
 * is a derived list — always computed from {@code labels} via
 * {@link LabelNormalizer#normalize}; never stored independently.</p>
 */
public final class CreditCard {

    public static final int MAX_NAME_LENGTH = 80;
    public static final String LEGACY_OWNER_ID = "legacy";
    public static final String SYNC_PLACEHOLDER_NAME = "card_sync";

    private final String id;
    private final String ownerId;
    private final String name;
    private final List<String> labels;
    private final List<String> normalizedLabels;

    public CreditCard(String id, String name) {
        this(id, name, LEGACY_OWNER_ID);
    }

    public CreditCard(String id, String name, String ownerId) {
        this(id, name, ownerId, List.of());
    }

    public CreditCard(String id, String name, String ownerId, List<String> labels) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerId = requireNonBlank(ownerId, "ownerId");
        this.name = validateName(name);
        this.labels = labels != null ? List.copyOf(labels) : List.of();
        this.normalizedLabels = this.labels.stream()
                .map(LabelNormalizer::normalize)
                .filter(l -> !l.isBlank())
                .distinct()
                .toList();
    }

    public static CreditCard create(String name) {
        return create(name, LEGACY_OWNER_ID);
    }

    public static CreditCard create(String name, String ownerId) {
        return new CreditCard(UUID.randomUUID().toString(), name, ownerId, List.of());
    }

    /**
     * Returns a new {@code CreditCard} with the given labels replacing the current ones.
     * {@code normalizedLabels} is recomputed automatically.
     */
    public CreditCard withLabels(List<String> labels) {
        return new CreditCard(this.id, this.name, this.ownerId, labels);
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<String> getNormalizedLabels() {
        return normalizedLabels;
    }

    private static String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (name.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "name must not exceed " + MAX_NAME_LENGTH + " characters");
        }
        return name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof CreditCard other
                && Objects.equals(id, other.id)
                && Objects.equals(ownerId, other.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ownerId);
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
