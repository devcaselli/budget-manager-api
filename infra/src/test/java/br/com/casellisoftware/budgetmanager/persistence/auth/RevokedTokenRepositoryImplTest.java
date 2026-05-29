package br.com.casellisoftware.budgetmanager.persistence.auth;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

@Import(RevokedTokenRepositoryImpl.class)
class RevokedTokenRepositoryImplTest extends AbstractMongoIntegrationTest {

    private static final String JTI       = "test-jti-uuid";
    private static final Instant EXPIRES  = Instant.now().plus(1, ChronoUnit.HOURS);

    @Autowired
    private RevokedTokenRepositoryImpl repository;

    // -----------------------------------------------------------------------
    // revoke
    // -----------------------------------------------------------------------

    @Test
    void revoke_persistsDocument() {
        repository.revoke(JTI, EXPIRES);

        assertThat(repository.isRevoked(JTI)).isTrue();
    }

    @Test
    void revoke_idempotent_noExceptionOnDuplicate() {
        repository.revoke(JTI, EXPIRES);
        repository.revoke(JTI, EXPIRES); // second call must not throw

        assertThat(repository.isRevoked(JTI)).isTrue();
    }

    // -----------------------------------------------------------------------
    // isRevoked
    // -----------------------------------------------------------------------

    @Test
    void isRevoked_unknownJti_returnsFalse() {
        assertThat(repository.isRevoked("unknown-jti")).isFalse();
    }

    @Test
    void isRevoked_afterRevoke_returnsTrue() {
        String jti2 = "another-jti";
        repository.revoke(jti2, EXPIRES);

        assertThat(repository.isRevoked(jti2)).isTrue();
    }

    @Test
    void isRevoked_differentJti_returnsFalse() {
        repository.revoke(JTI, EXPIRES);

        assertThat(repository.isRevoked("completely-different-jti")).isFalse();
    }
}
