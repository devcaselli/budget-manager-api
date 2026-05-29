package br.com.casellisoftware.budgetmanager.application.flag;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FlagStrategyRegistryTest {

    @Test
    void resolve_usesDefaultForNoneAndRegisteredStrategyForEnabledFlag() {
        FlagStrategy<String, String> defaultStrategy = input -> "default:" + input;
        FlagStrategy<String, String> alternativeStrategy = input -> "alternative:" + input;
        FlagStrategyRegistry<String, String> registry = new FlagStrategyRegistry<>(
                defaultStrategy,
                Map.of(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, alternativeStrategy)
        );

        assertThat(registry.resolve(FlagEnum.NONE).apply("x")).isEqualTo("default:x");
        assertThat(registry.resolve(null).apply("x")).isEqualTo("default:x");
        assertThat(registry.resolve(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION).apply("x"))
                .isEqualTo("alternative:x");
    }

    // -----------------------------------------------------------------------
    // Builder tests
    // -----------------------------------------------------------------------

    @Test
    void builder_resolvesDefault_forNoneAndUnregistered() {
        FlagStrategy<String, String> defaultStrategy = input -> "default:" + input;
        FlagStrategyRegistry<String, String> registry = FlagStrategyRegistry.<String, String>builder()
                .withDefault(defaultStrategy)
                .build();

        assertThat(registry.resolve(FlagEnum.NONE).apply("x")).isEqualTo("default:x");
        assertThat(registry.resolve(null).apply("x")).isEqualTo("default:x");
        assertThat(registry.resolve(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION).apply("x"))
                .isEqualTo("default:x");
    }

    @Test
    void builder_resolvesRegistered_forKnownFlag() {
        FlagStrategy<String, String> defaultStrategy = input -> "default:" + input;
        FlagStrategy<String, String> specific = input -> "specific:" + input;
        FlagStrategyRegistry<String, String> registry = FlagStrategyRegistry.<String, String>builder()
                .withDefault(defaultStrategy)
                .register(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION, specific)
                .build();

        assertThat(registry.resolve(FlagEnum.BULLET_IGNORE_SUBSCRIPTION_RESERVATION).apply("x"))
                .isEqualTo("specific:x");
        // Unregistered flag still falls back to default
        assertThat(registry.resolve(FlagEnum.SUBSCRIPTION_DELETE_IGNORE_DATE_VALIDATION).apply("x"))
                .isEqualTo("default:x");
    }

    @Test
    void builder_throwsWhenDefaultMissing() {
        assertThatThrownBy(() ->
                FlagStrategyRegistry.<String, String>builder().build()
        ).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("default strategy");
    }

    @Test
    void builder_throwsWhenRegisteringNone() {
        FlagStrategy<String, String> strategy = input -> input;
        assertThatThrownBy(() ->
                FlagStrategyRegistry.<String, String>builder()
                        .withDefault(strategy)
                        .register(FlagEnum.NONE, strategy)
        ).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("NONE");
    }
}
