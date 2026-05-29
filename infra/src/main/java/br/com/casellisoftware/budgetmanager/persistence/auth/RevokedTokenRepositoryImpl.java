package br.com.casellisoftware.budgetmanager.persistence.auth;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.TokenRevocationPort;
import lombok.RequiredArgsConstructor;

import java.time.Instant;

@RequiredArgsConstructor
public class RevokedTokenRepositoryImpl implements TokenRevocationPort {

    private final RevokedTokenMongoRepository mongoRepository;

    @Override
    public void revoke(String jti, Instant expiresAt) {
        mongoRepository.save(new RevokedTokenDocument(jti, expiresAt));
    }

    @Override
    public boolean isRevoked(String jti) {
        return mongoRepository.existsById(jti);
    }
}
