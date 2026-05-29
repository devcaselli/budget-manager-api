package br.com.casellisoftware.budgetmanager.configs;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshBoundary;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenRevocationPort;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenGeneratorPort;
import br.com.casellisoftware.budgetmanager.application.auth.usecase.RefreshUseCase;
import br.com.casellisoftware.budgetmanager.configs.security.JtiRevocationValidator;
import br.com.casellisoftware.budgetmanager.persistence.auth.RefreshTokenMongoRepository;
import br.com.casellisoftware.budgetmanager.persistence.auth.RefreshTokenRepositoryImpl;
import br.com.casellisoftware.budgetmanager.persistence.auth.RevokedTokenMongoRepository;
import br.com.casellisoftware.budgetmanager.persistence.auth.RevokedTokenRepositoryImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AuthBeanConfiguration {

    @Bean
    public TokenRevocationPort tokenRevocationPort(RevokedTokenMongoRepository mongoRepository) {
        return new RevokedTokenRepositoryImpl(mongoRepository);
    }

    @Bean
    public JtiRevocationValidator jtiRevocationValidator(TokenRevocationPort tokenRevocationPort) {
        return new JtiRevocationValidator(tokenRevocationPort);
    }

    @Bean
    public RefreshTokenPort refreshTokenPort(RefreshTokenMongoRepository mongoRepository) {
        return new RefreshTokenRepositoryImpl(mongoRepository);
    }

    @Bean
    public RefreshBoundary refreshBoundary(
            RefreshTokenPort refreshTokenPort,
            TokenGeneratorPort tokenGeneratorPort,
            @Value("${app.jwt.refresh-expiration-seconds:604800}") long refreshExpirationSeconds) {
        return new RefreshUseCase(refreshTokenPort, tokenGeneratorPort, refreshExpirationSeconds);
    }
}
