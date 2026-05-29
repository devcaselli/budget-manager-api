package br.com.casellisoftware.budgetmanager.application.flag;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class FlagStrategyRegistry<I, O> {

    private final FlagStrategy<I, O> defaultStrategy;
    private final Map<FlagEnum, FlagStrategy<I, O>> strategies;

    public FlagStrategyRegistry(FlagStrategy<I, O> defaultStrategy,
                                Map<FlagEnum, FlagStrategy<I, O>> strategies) {
        this.defaultStrategy = Objects.requireNonNull(defaultStrategy, "defaultStrategy must not be null");
        this.strategies = new EnumMap<>(FlagEnum.class);
        if (strategies != null) {
            this.strategies.putAll(strategies);
        }
    }

    public FlagStrategy<I, O> resolve(FlagEnum flag) {
        if (flag == null || flag == FlagEnum.NONE) {
            return defaultStrategy;
        }
        return strategies.getOrDefault(flag, defaultStrategy);
    }

    /**
     * Returns a new {@link Builder} for constructing a {@code FlagStrategyRegistry}.
     *
     * @param <I> use-case input type
     * @param <O> use-case output type
     * @return a fresh builder instance
     */
    public static <I, O> Builder<I, O> builder() {
        return new Builder<>();
    }

    /**
     * Fluent builder for {@link FlagStrategyRegistry}. Centralises the
     * {@link EnumMap} allocation so callers never need to construct a raw map.
     *
     * @param <I> use-case input type
     * @param <O> use-case output type
     */
    public static final class Builder<I, O> {

        private FlagStrategy<I, O> defaultStrategy;
        private final Map<FlagEnum, FlagStrategy<I, O>> strategies = new EnumMap<>(FlagEnum.class);

        private Builder() {
        }

        /**
         * Sets the fallback strategy used when the resolved flag is {@link FlagEnum#NONE}
         * or has no explicit registration.
         *
         * @param strategy the default strategy (must not be {@code null})
         * @return this builder for chaining
         */
        public Builder<I, O> withDefault(FlagStrategy<I, O> strategy) {
            this.defaultStrategy = Objects.requireNonNull(strategy, "default strategy must not be null");
            return this;
        }

        /**
         * Registers a strategy for a specific flag value.
         *
         * @param flag     the flag that selects this strategy (must not be {@code null} or {@link FlagEnum#NONE})
         * @param strategy the strategy to invoke when the flag is active (must not be {@code null})
         * @return this builder for chaining
         */
        public Builder<I, O> register(FlagEnum flag, FlagStrategy<I, O> strategy) {
            Objects.requireNonNull(flag, "flag must not be null");
            Objects.requireNonNull(strategy, "strategy must not be null");
            if (flag == FlagEnum.NONE) {
                throw new IllegalArgumentException(
                        "flag NONE has no strategy mapping; use withDefault(...) instead");
            }
            strategies.put(flag, strategy);
            return this;
        }

        /**
         * Builds the {@link FlagStrategyRegistry}.
         *
         * @return a new registry
         * @throws IllegalStateException if no default strategy has been set
         */
        public FlagStrategyRegistry<I, O> build() {
            if (defaultStrategy == null) {
                throw new IllegalStateException("A default strategy must be set via withDefault(...)");
            }
            return new FlagStrategyRegistry<>(defaultStrategy, strategies);
        }
    }
}
