package br.com.casellisoftware.budgetmanager.application.pluggy.usecase;

import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.CreateConnectTokenBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.ConnectTokenOutput;
import br.com.casellisoftware.budgetmanager.domain.pluggy.ConnectToken;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionRepository;

import java.util.Objects;

/**
 * Creates a Pluggy Connect Token for the authenticated owner by delegating to the
 * {@link PluggyClient} port. The owner id is forwarded as Pluggy's {@code clientUserId}
 * so downstream item/webhook events can be correlated back to the user.
 *
 * <p>When {@code itemId} is present (Connect widget <em>update mode</em> — re-sync of an
 * already-connected item), ownership is verified first via {@link PluggyConnectionRepository}
 * so a user cannot mint an update token for someone else's item.</p>
 */
public class CreateConnectTokenUseCase implements CreateConnectTokenBoundary {

    private final PluggyClient pluggyClient;
    private final PluggyConnectionRepository pluggyConnectionRepository;

    public CreateConnectTokenUseCase(PluggyClient pluggyClient,
                                      PluggyConnectionRepository pluggyConnectionRepository) {
        this.pluggyClient = Objects.requireNonNull(pluggyClient, "pluggyClient must not be null");
        this.pluggyConnectionRepository =
                Objects.requireNonNull(pluggyConnectionRepository, "pluggyConnectionRepository must not be null");
    }

    @Override
    public ConnectTokenOutput execute(String ownerId, String itemId) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");

        if (itemId != null) {
            pluggyConnectionRepository.findByItemIdAndOwnerId(itemId, ownerId)
                    .orElseThrow(() -> new PluggyConnectionNotFoundException(itemId, ownerId));
        }

        ConnectToken token = pluggyClient.createConnectToken(ownerId, itemId);
        return new ConnectTokenOutput(token.accessToken());
    }
}
