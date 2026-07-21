package br.com.casellisoftware.budgetmanager.application.pluggy.usecase;

import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyAccount;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnection;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionRepository;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyTransaction;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Shared helper: resolves an owner-scoped {@link PluggyConnection} and fetches the raw
 * {@link PluggyTransaction}s across all of its accounts for a date range, with each
 * transaction's {@code accountType} resolved and attached.
 *
 * <p>Used by both {@link FindPluggyTransactionsUseCase} (preview) and
 * {@link MaterializePluggyTransactionsUseCase} (materialize) so the two stay in sync on
 * what "the transactions for this item/range" means — preview must show exactly what
 * materialize would act on. Centralizing the {@code accountType} resolution here (rather
 * than in each use case) guarantees both paths classify {@code isExpense()} identically.</p>
 *
 * <p>{@code accountType} (e.g. {@code BANK}/{@code CREDIT}) is not persisted on {@link
 * PluggyConnection} — only {@code accountIds} are — so it is resolved via one extra
 * {@code PluggyClient#listAccounts} call per fetch, rather than reworking persistence.</p>
 *
 * @implNote Time complexity: O(n + a * t) where n = accounts on the item (one {@code
 *           listAccounts} call), a = accounts, t = transactions per account per range (one
 *           Pluggy call per account). Space complexity: O(n + a * t).
 */
class FetchPluggyTransactionsForItemUseCase {

    private final PluggyClient pluggyClient;
    private final PluggyConnectionRepository pluggyConnectionRepository;

    FetchPluggyTransactionsForItemUseCase(PluggyClient pluggyClient,
                                           PluggyConnectionRepository pluggyConnectionRepository) {
        this.pluggyClient = Objects.requireNonNull(pluggyClient, "pluggyClient must not be null");
        this.pluggyConnectionRepository = Objects.requireNonNull(pluggyConnectionRepository, "pluggyConnectionRepository must not be null");
    }

    /**
     * @throws PluggyConnectionNotFoundException if no connection scoped to {@code ownerId}
     *                                            exists for {@code itemId}
     */
    List<PluggyTransaction> fetch(String ownerId, String itemId, LocalDate from, LocalDate to) {
        PluggyConnection connection = pluggyConnectionRepository.findByItemIdAndOwnerId(itemId, ownerId)
                .orElseThrow(() -> new PluggyConnectionNotFoundException(itemId, ownerId));

        Map<String, String> accountTypesById = pluggyClient.listAccounts(itemId).stream()
                .collect(Collectors.toMap(PluggyAccount::id, PluggyAccount::type, (a, b) -> a));

        return connection.getAccountIds().stream()
                .flatMap(accountId -> pluggyClient.listTransactions(accountId, from, to).stream()
                        .map(tx -> tx.withAccountType(accountTypesById.get(accountId))))
                .toList();
    }
}
