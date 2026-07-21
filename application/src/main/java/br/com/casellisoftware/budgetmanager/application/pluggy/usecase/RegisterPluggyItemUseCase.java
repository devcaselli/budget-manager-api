package br.com.casellisoftware.budgetmanager.application.pluggy.usecase;

import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.RegisterPluggyItemBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyConnectionOutput;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyAccount;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnection;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionRepository;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Registers (or refreshes) the link between an owner and a Pluggy {@code item}.
 *
 * <p>Invoked as the frontend widget callback: after the Pluggy Connect widget creates
 * an item client-side, the frontend posts the {@code itemId} here. This use case fetches
 * the item + its accounts and persists/updates the {@link PluggyConnection}.</p>
 *
 * <p>Idempotent: registering the same {@code itemId} again for the same owner updates
 * the existing connection (status/accountIds/updatedAt) instead of creating a duplicate.</p>
 *
 * @implNote Time complexity: O(a) where a = number of accounts on the item (one Pluggy
 *           call each way; the account list itself is small, bounded by the institution).
 */
public class RegisterPluggyItemUseCase implements RegisterPluggyItemBoundary {

    private static final Logger log = LoggerFactory.getLogger(RegisterPluggyItemUseCase.class);

    private final PluggyClient pluggyClient;
    private final PluggyConnectionRepository pluggyConnectionRepository;
    private final Clock clock;

    public RegisterPluggyItemUseCase(PluggyClient pluggyClient,
                                      PluggyConnectionRepository pluggyConnectionRepository,
                                      Clock clock) {
        this.pluggyClient = Objects.requireNonNull(pluggyClient, "pluggyClient must not be null");
        this.pluggyConnectionRepository = Objects.requireNonNull(pluggyConnectionRepository, "pluggyConnectionRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public PluggyConnectionOutput execute(String ownerId, String itemId) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");

        PluggyItem item = pluggyClient.getItem(itemId);
        List<PluggyAccount> accounts = pluggyClient.listAccounts(itemId);
        List<String> accountIds = accounts.stream().map(PluggyAccount::id).toList();

        Instant now = clock.instant();
        PluggyConnection connection = pluggyConnectionRepository.findByItemIdAndOwnerId(itemId, ownerId)
                .map(existing -> existing.withAccountsAndStatus(item.status(), accountIds, now))
                .orElseGet(() -> PluggyConnection.create(ownerId, itemId, item.connectorId(), item.status(), accountIds, now));

        PluggyConnection saved = pluggyConnectionRepository.save(connection);
        log.info("Registered Pluggy connection connectionId={} ownerId={} itemId={} accounts={}",
                saved.getId(), ownerId, itemId, accountIds.size());

        return toOutput(saved);
    }

    static PluggyConnectionOutput toOutput(PluggyConnection connection) {
        return new PluggyConnectionOutput(
                connection.getId(),
                connection.getItemId(),
                connection.getConnectorId(),
                connection.getStatus(),
                connection.getAccountIds(),
                connection.getCreatedAt(),
                connection.getUpdatedAt());
    }
}
