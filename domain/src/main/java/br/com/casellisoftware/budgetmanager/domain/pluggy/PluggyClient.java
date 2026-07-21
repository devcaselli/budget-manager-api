package br.com.casellisoftware.budgetmanager.domain.pluggy;

import java.time.LocalDate;
import java.util.List;

/**
 * Outbound port to the Pluggy API.
 *
 * <p>Implementations are responsible for the backend-only authentication handshake
 * ({@code POST /auth} with clientId + clientSecret to obtain a short-lived {@code apiKey})
 * and for never exposing those secrets or the {@code apiKey} to callers. Only the
 * resulting {@link ConnectToken} is safe to return toward the frontend.</p>
 */
public interface PluggyClient {

    /**
     * Creates a Pluggy Connect Token for a frontend Connect session (new-connection flow).
     *
     * @param clientUserId stable identifier of the end user (the JWT subject / ownerId),
     *                     forwarded to Pluggy so events can be correlated to the user
     * @return a freshly minted, short-lived connect token
     */
    default ConnectToken createConnectToken(String clientUserId) {
        return createConnectToken(clientUserId, null);
    }

    /**
     * Creates a Pluggy Connect Token, optionally scoped to an existing {@code itemId} for
     * the Connect widget's <em>update mode</em> (user-triggered re-sync of an already
     * connected item, e.g. to pull recent credit-card transactions that lag).
     *
     * @param clientUserId stable identifier of the end user (the JWT subject / ownerId),
     *                     forwarded to Pluggy so events can be correlated to the user
     * @param itemId       Pluggy item id to scope the token to for update mode, or
     *                     {@code null} for a brand-new connection
     * @return a freshly minted, short-lived connect token
     */
    ConnectToken createConnectToken(String clientUserId, String itemId);

    /**
     * Fetches a single Pluggy {@code item} by id.
     *
     * @param itemId Pluggy item id (created client-side by the Pluggy Connect widget)
     * @return the item, including its current sync status
     */
    PluggyItem getItem(String itemId);

    /**
     * Lists the accounts (bank accounts / credit cards) belonging to a Pluggy {@code item}.
     *
     * @param itemId Pluggy item id
     * @return accounts for this item; empty if none
     */
    List<PluggyAccount> listAccounts(String itemId);

    /**
     * Lists transactions for a Pluggy account within a date range.
     *
     * @param accountId Pluggy account id
     * @param from      inclusive start date
     * @param to        inclusive end date
     * @return transactions in the given range; empty if none
     */
    List<PluggyTransaction> listTransactions(String accountId, LocalDate from, LocalDate to);
}
