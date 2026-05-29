package br.com.casellisoftware.budgetmanager.configs.security;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenGeneratorPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenOutput;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.security.enabled", havingValue = "false")
public class DisabledSecurityTokenGenerator implements TokenGeneratorPort {

    @Override
    public TokenOutput generate(String userId, String email) {
        return new TokenOutput("security-disabled", "Bearer", 0, null, 0);
    }
}
