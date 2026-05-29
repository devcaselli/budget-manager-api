package br.com.casellisoftware.budgetmanager.domain.sync;

import java.util.Objects;

/**
 * Domain entity representing an owner's ingest-sync preference.
 *
 * <p>When {@code enabled} is {@code true}, the nightly cron job includes this owner
 * in its sync run. Owners are opt-in by default ({@code enabled = true}) and can
 * disable auto-sync at any time.</p>
 */
public final class SyncPreference {

    private final String ownerId;
    private final boolean enabled;

    public SyncPreference(String ownerId, boolean enabled) {
        this.ownerId = Objects.requireNonNull(ownerId, "ownerId must not be null");
        if (ownerId.isBlank()) {
            throw new IllegalArgumentException("ownerId must not be blank");
        }
        this.enabled = enabled;
    }

    public static SyncPreference defaultFor(String ownerId) {
        return new SyncPreference(ownerId, true);
    }

    public SyncPreference withEnabled(boolean enabled) {
        return new SyncPreference(this.ownerId, enabled);
    }

    public String getOwnerId() {
        return ownerId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof SyncPreference sp && Objects.equals(ownerId, sp.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerId);
    }
}
