package br.com.casellisoftware.budgetmanager.persistence.auth;

import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenData;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenPort;
import lombok.RequiredArgsConstructor;

import java.time.Instant;
import java.util.Optional;

@RequiredArgsConstructor
public class RefreshTokenRepositoryImpl implements RefreshTokenPort {

    private final RefreshTokenMongoRepository mongoRepository;

    @Override
    public void save(String token, String userId, String email, Instant expiresAt) {
        mongoRepository.save(new RefreshTokenDocument(token, userId, email, expiresAt));
    }

    @Override
    public Optional<RefreshTokenData> findByToken(String token) {
        return mongoRepository.findByToken(token)
                .map(doc -> new RefreshTokenData(doc.getUserId(), doc.getEmail()));
    }

    @Override
    public void delete(String token) {
        mongoRepository.deleteByToken(token);
    }
}
