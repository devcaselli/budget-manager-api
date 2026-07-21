package br.com.casellisoftware.budgetmanager.domain.pluggy;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing the persisted link between an owner and a Pluggy
 * {@code item} (a connected bank).
 *
 * <p>Immutable: every state-changing operation returns a new instance. Created via
 * {@link #create(String, String, String, List, Instant)} on first registration
 * ({@code POST /pluggy/items}) and refreshed via {@link #withAccountsAndStatus} on
 * subsequent registrations of the same {@code itemId} (idempotent upsert).</p>
 */
public final class PluggyConnection {

    private final String id;
    private final String ownerId;
    private final String itemId;
    private final String connectorId;
    private final String status;
    private final List<String> accountIds;
    private final Instant createdAt;
    private final Instant updatedAt;

    public PluggyConnection(String id,
                             String ownerId,
                             String itemId,
                             String connectorId,
                             String status,
                             List<String> accountIds,
                             Instant createdAt,
                             Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerId = requireNonBlank(ownerId, "ownerId");
        this.itemId = requireNonBlank(itemId, "itemId");
        this.connectorId = connectorId;
        this.status = status;
        this.accountIds = accountIds != null ? List.copyOf(accountIds) : List.of();
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    /** Creates a brand-new connection, generating a fresh id. */
    public static PluggyConnection create(String ownerId,
                                           String itemId,
                                           String connectorId,
                                           String status,
                                           List<String> accountIds,
                                           Instant now) {
        return new PluggyConnection(
                UUID.randomUUID().toString(), ownerId, itemId, connectorId, status, accountIds, now, now);
    }

    /**
     * Returns a new {@code PluggyConnection} with refreshed {@code accountIds}/{@code status}
     * and {@code updatedAt}, preserving id/ownerId/itemId/createdAt. Used for the idempotent
     * upsert path when the owner registers the same {@code itemId} again.
     */
    public PluggyConnection withAccountsAndStatus(String status, List<String> accountIds, Instant now) {
        return new PluggyConnection(this.id, this.ownerId, this.itemId, this.connectorId,
                status, accountIds, this.createdAt, now);
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getItemId() {
        return itemId;
    }

    public String getConnectorId() {
        return connectorId;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getAccountIds() {
        return accountIds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof PluggyConnection other
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
