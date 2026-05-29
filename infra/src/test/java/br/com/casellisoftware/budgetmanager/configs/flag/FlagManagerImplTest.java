package br.com.casellisoftware.budgetmanager.configs.flag;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class FlagManagerImplTest {

    @Test
    void isEnabled_returnsFalseForNoneAndMissingFlagsAndTrueForConfiguredFlags() {
        FlagsProperties properties = new FlagsProperties();
        properties.setValues(Map.of(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, true));

        FlagManagerImpl manager = new FlagManagerImpl(properties);

        assertThat(manager.isEnabled(FlagEnum.NONE)).isFalse();
        assertThat(manager.isEnabled(null)).isFalse();
        assertThat(manager.isEnabled(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION)).isTrue();
    }
}
