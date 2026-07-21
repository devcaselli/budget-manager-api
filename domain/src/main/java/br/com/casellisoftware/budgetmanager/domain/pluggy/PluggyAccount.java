package br.com.casellisoftware.budgetmanager.domain.pluggy;

import java.util.Objects;

/**
 * A Pluggy {@code account} — a bank account or credit card belonging to an {@code item}.
 *
 * @param id     Pluggy account id
 * @param itemId id of the parent {@link PluggyItem}
 * @param name   account display name (e.g. "Conta Corrente")
 * @param type   Pluggy account type (e.g. {@code BANK}, {@code CREDIT})
 */
public record PluggyAccount(String id, String itemId, String name, String type) {

    public PluggyAccount {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");
    }
}
