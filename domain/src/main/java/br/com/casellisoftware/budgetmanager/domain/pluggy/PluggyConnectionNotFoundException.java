package br.com.casellisoftware.budgetmanager.domain.pluggy;

/**
 * Thrown when an owner references an {@code itemId} that has no {@link PluggyConnection}
 * scoped to them — either it does not exist, or it belongs to a different owner.
 */
public class PluggyConnectionNotFoundException extends RuntimeException {

    public PluggyConnectionNotFoundException(String itemId, String ownerId) {
        super("No Pluggy connection found for itemId=" + itemId + " ownerId=" + ownerId);
    }
}
