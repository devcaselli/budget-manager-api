package br.com.casellisoftware.budgetmanager.application.flag;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlagAwareExecutorTest {

    @Mock
    private FlagManager flagManager;

    private FlagAwareExecutor<String, String> executor;

    @BeforeEach
    void setUp() {
        FlagStrategy<String, String> defaultStrategy = input -> "default:" + input;
        FlagStrategy<String, String> alternativeStrategy = input -> "alternative:" + input;
        FlagStrategyRegistry<String, String> registry = new FlagStrategyRegistry<>(
                defaultStrategy,
                Map.of(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, alternativeStrategy)
        );
        executor = new FlagAwareExecutor<>(flagManager, registry);
    }

    @Test
    void execute_usesDefaultStrategyWhenFlagIsDisabled() {
        when(flagManager.isEnabled(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION)).thenReturn(false);

        String result = executor.execute(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, "payload");

        assertThat(result).isEqualTo("default:payload");
        verify(flagManager).isEnabled(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);
    }

    @Test
    void execute_usesRegisteredStrategyWhenFlagIsEnabled() {
        when(flagManager.isEnabled(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION)).thenReturn(true);

        String result = executor.execute(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, "payload");

        assertThat(result).isEqualTo("alternative:payload");
        verify(flagManager).isEnabled(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION);
    }
}
