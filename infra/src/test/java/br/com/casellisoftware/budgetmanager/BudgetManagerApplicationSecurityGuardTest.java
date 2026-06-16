package br.com.casellisoftware.budgetmanager;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BudgetManagerApplicationSecurityGuardTest {

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private BudgetManagerApplication app(String profiles, String securityEnabled) {
        return app(profiles, securityEnabled, null);
    }

    private BudgetManagerApplication app(String profiles, String securityEnabled, String autoIndexCreation) {
        MockEnvironment env = new MockEnvironment();
        if (profiles != null) env.setActiveProfiles(profiles.split(","));
        if (securityEnabled != null) env.setProperty("app.security.enabled", securityEnabled);
        if (autoIndexCreation != null) env.setProperty("spring.data.mongodb.auto-index-creation", autoIndexCreation);
        return new BudgetManagerApplication(env);
    }

    // -----------------------------------------------------------------------
    // assertSecurityEnabledInProd
    // -----------------------------------------------------------------------

    @Test
    void prod_securityEnabled_noException() {
        assertThatNoException().isThrownBy(app("prod", "true")::assertSecurityEnabledInProd);
    }

    @Test
    void prod_securityDisabled_throwsIllegalState() {
        assertThatThrownBy(app("prod", "false")::assertSecurityEnabledInProd)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Security MUST be enabled in production");
    }

    @Test
    void dev_securityDisabled_noException() {
        assertThatNoException().isThrownBy(app("dev", "false")::assertSecurityEnabledInProd);
    }

    @Test
    void noprofile_securityDisabled_noException() {
        assertThatNoException().isThrownBy(app(null, "false")::assertSecurityEnabledInProd);
    }

    @Test
    void prod_securityPropertyAbsent_defaultsToEnabledNoException() {
        assertThatNoException().isThrownBy(app("prod", null)::assertSecurityEnabledInProd);
    }

    // -----------------------------------------------------------------------
    // assertAutoIndexCreationDisabledInProd (P-3)
    // -----------------------------------------------------------------------

    @Test
    void prod_autoIndexDisabled_noException() {
        assertThatNoException()
                .isThrownBy(app("prod", "true", "false")::assertAutoIndexCreationDisabledInProd);
    }

    @Test
    void prod_autoIndexEnabled_throwsIllegalState() {
        assertThatThrownBy(app("prod", "true", "true")::assertAutoIndexCreationDisabledInProd)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("auto-index-creation MUST be false in production");
    }

    @Test
    void dev_autoIndexEnabled_noException() {
        assertThatNoException()
                .isThrownBy(app("dev", "true", "true")::assertAutoIndexCreationDisabledInProd);
    }

    @Test
    void noprofile_autoIndexEnabled_noException() {
        assertThatNoException()
                .isThrownBy(app(null, "true", "true")::assertAutoIndexCreationDisabledInProd);
    }

    @Test
    void prod_autoIndexPropertyAbsent_noException() {
        // property not set → defaults to false → guard must not fire
        assertThatNoException()
                .isThrownBy(app("prod", "true", null)::assertAutoIndexCreationDisabledInProd);
    }
}
