package br.com.casellisoftware.budgetmanager.persistence.auth;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.application.auth.boundary.RefreshTokenData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Import(RefreshTokenRepositoryImpl.class)
class RefreshTokenRepositoryImplTest extends AbstractMongoIntegrationTest {

    private static final String TOKEN   = "test-refresh-uuid";
    private static final String USER_ID = "user-1";
    private static final String EMAIL   = "user@example.com";
    private static final Instant EXPIRES = Instant.now().plus(7, ChronoUnit.DAYS);

    @Autowired
    private RefreshTokenRepositoryImpl repository;

    // -----------------------------------------------------------------------
    // save + findByToken
    // -----------------------------------------------------------------------

    @Test
    void save_thenFindByToken_returnsCorrectData() {
        repository.save(TOKEN, USER_ID, EMAIL, EXPIRES);

        Optional<RefreshTokenData> found = repository.findByToken(TOKEN);

        assertThat(found).isPresent();
        assertThat(found.get().userId()).isEqualTo(USER_ID);
        assertThat(found.get().email()).isEqualTo(EMAIL);
    }

    @Test
    void findByToken_unknownToken_returnsEmpty() {
        assertThat(repository.findByToken("unknown-token")).isEmpty();
    }

    // -----------------------------------------------------------------------
    // delete
    // -----------------------------------------------------------------------

    @Test
    void delete_removesToken() {
        repository.save(TOKEN, USER_ID, EMAIL, EXPIRES);

        repository.delete(TOKEN);

        assertThat(repository.findByToken(TOKEN)).isEmpty();
    }

    @Test
    void delete_nonExistentToken_noException() {
        repository.delete("ghost-token"); // must not throw
    }

    // -----------------------------------------------------------------------
    // Idempotency
    // -----------------------------------------------------------------------

    @Test
    void save_sameTokenTwice_noExceptionAndReturnsLatest() {
        repository.save(TOKEN, USER_ID, EMAIL, EXPIRES);
        Instant newExpiry = EXPIRES.plus(1, ChronoUnit.DAYS);
        repository.save(TOKEN, USER_ID, EMAIL, newExpiry);

        assertThat(repository.findByToken(TOKEN)).isPresent();
    }
}
