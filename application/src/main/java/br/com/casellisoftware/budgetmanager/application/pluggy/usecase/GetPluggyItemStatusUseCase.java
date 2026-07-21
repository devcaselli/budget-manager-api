package br.com.casellisoftware.budgetmanager.application.pluggy.usecase;

import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.GetPluggyItemStatusBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyItemStatusOutput;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionRepository;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyItem;

import java.util.Objects;

/**
 * Fetches the current sync status of an owner-scoped Pluggy {@code item}, for the
 * frontend to poll after triggering an update-mode re-sync via the Connect widget.
 *
 * <p>Ownership is verified first via {@link PluggyConnectionRepository} — a user may only
 * poll the status of items they own.</p>
 *
 * @implNote Time complexity: O(1) (one ownership lookup + one Pluggy {@code GET /items/{id}}
 *           call). Space complexity: O(1).
 */
public class GetPluggyItemStatusUseCase implements GetPluggyItemStatusBoundary {

    private final PluggyClient pluggyClient;
    private final PluggyConnectionRepository pluggyConnectionRepository;

    public GetPluggyItemStatusUseCase(PluggyClient pluggyClient,
                                       PluggyConnectionRepository pluggyConnectionRepository) {
        this.pluggyClient = Objects.requireNonNull(pluggyClient, "pluggyClient must not be null");
        this.pluggyConnectionRepository =
                Objects.requireNonNull(pluggyConnectionRepository, "pluggyConnectionRepository must not be null");
    }

    @Override
    public PluggyItemStatusOutput execute(String ownerId, String itemId) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        Objects.requireNonNull(itemId, "itemId must not be null");

        pluggyConnectionRepository.findByItemIdAndOwnerId(itemId, ownerId)
                .orElseThrow(() -> new PluggyConnectionNotFoundException(itemId, ownerId));

        PluggyItem item = pluggyClient.getItem(itemId);
        return new PluggyItemStatusOutput(item.status());
    }
}
