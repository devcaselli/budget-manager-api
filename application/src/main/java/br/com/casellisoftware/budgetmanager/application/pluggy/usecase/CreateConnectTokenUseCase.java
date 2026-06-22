package br.com.casellisoftware.budgetmanager.application.pluggy.usecase;

import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.CreateConnectTokenBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.ConnectTokenOutput;
import br.com.casellisoftware.budgetmanager.domain.pluggy.ConnectToken;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;

import java.util.Objects;

/**
 * Creates a Pluggy Connect Token for the authenticated owner by delegating to the
 * {@link PluggyClient} port. The owner id is forwarded as Pluggy's {@code clientUserId}
 * so downstream item/webhook events can be correlated back to the user.
 */
public class CreateConnectTokenUseCase implements CreateConnectTokenBoundary {

    private final PluggyClient pluggyClient;

    public CreateConnectTokenUseCase(PluggyClient pluggyClient) {
        this.pluggyClient = Objects.requireNonNull(pluggyClient, "pluggyClient must not be null");
    }

    @Override
    public ConnectTokenOutput execute(String ownerId) {
        Objects.requireNonNull(ownerId, "ownerId must not be null");
        ConnectToken token = pluggyClient.createConnectToken(ownerId);
        return new ConnectTokenOutput(token.accessToken());
    }
}
