package br.com.casellisoftware.budgetmanager;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class BudgetManagerApplicationSecurityGuardTest {

    private BudgetManagerApplication appWithProfiles(String profiles, String securityEnabled) {
        MockEnvironment env = new MockEnvironment();
        if (profiles != null) env.setActiveProfiles(profiles.split(","));
        if (securityEnabled != null) env.setProperty("app.security.enabled", securityEnabled);
        return new BudgetManagerApplication(env);
    }

    @Test
    void prod_securityEnabled_noException() {
        BudgetManagerApplication app = appWithProfiles("prod", "true");
        assertThatNoException().isThrownBy(app::assertSecurityEnabledInProd);
    }

    @Test
    void prod_securityDisabled_throwsIllegalState() {
        BudgetManagerApplication app = appWithProfiles("prod", "false");
        assertThatThrownBy(app::assertSecurityEnabledInProd)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Security MUST be enabled in production");
    }

    @Test
    void dev_securityDisabled_noException() {
        BudgetManagerApplication app = appWithProfiles("dev", "false");
        assertThatNoException().isThrownBy(app::assertSecurityEnabledInProd);
    }

    @Test
    void noprofile_securityDisabled_noException() {
        BudgetManagerApplication app = appWithProfiles(null, "false");
        assertThatNoException().isThrownBy(app::assertSecurityEnabledInProd);
    }

    @Test
    void prod_securityPropertyAbsent_defaultsToEnabledNoException() {
        BudgetManagerApplication app = appWithProfiles("prod", null);
        assertThatNoException().isThrownBy(app::assertSecurityEnabledInProd);
    }
}
