package br.com.casellisoftware.budgetmanager.rest.pluggy;

import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.CreateConnectTokenBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.ConnectTokenOutput;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.rest.pluggy.dtos.ConnectTokenResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exposes the Pluggy Connect Token to the authenticated frontend so it can open the
 * Pluggy Connect widget. The backend {@code apiKey} and Pluggy secrets never leave the
 * server — only the short-lived connect token is returned here.
 *
 * <p>Secured by the default authenticated filter chain; {@link AuthenticatedUser} is
 * resolved from the JWT subject.</p>
 */
@RestController
@RequestMapping("/pluggy")
@RequiredArgsConstructor
public class PluggyConnectController {

    private final CreateConnectTokenBoundary createConnectTokenBoundary;

    @PostMapping("/connect-token")
    public ResponseEntity<ConnectTokenResponseDto> createConnectToken(AuthenticatedUser authenticatedUser) {
        ConnectTokenOutput output = createConnectTokenBoundary.execute(authenticatedUser.ownerId());
        return ResponseEntity.ok(new ConnectTokenResponseDto(output.connectToken()));
    }
}
