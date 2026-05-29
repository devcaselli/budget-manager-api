package br.com.casellisoftware.budgetmanager.application.flag;

/**
 * A single behavioural variant selected at runtime by {@link FlagAwareExecutor}
 * based on a domain entity's active {@link br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum}.
 *
 * <p>A {@code FlagStrategy} encapsulates one branch of conditional logic in a use case.
 * For example, {@code SaveBulletIgnoreSubscriptionReservationStrategy} implements the
 * "save bullet without checking subscription limits" variant. {@link FlagAwareExecutor}
 * resolves the effective flag at runtime and dispatches to the matching strategy via
 * {@link FlagStrategyRegistry}.</p>
 *
 * <h2>Contract</h2>
 * <p>Implementations <strong>MUST</strong> satisfy the following requirements:</p>
 * <ul>
 *   <li><strong>Decision Determinism:</strong> The branch taken by a strategy (e.g. which
 *       policy is applied, which validation is invoked) must be determined solely by the
 *       input and the strategy's dependencies. No hidden mutable state must influence the
 *       decision branch between consecutive calls. However, strategies MAY produce different
 *       return values across calls when the underlying domain naturally generates fresh
 *       identity (e.g., {@code UUID}s for new aggregates) or reads from time-dependent
 *       sources (e.g., clock). These variations are acceptable because they are inherent
 *       to the protected operation, not hidden state.</li>
 *   <li><strong>No unrelated side effects:</strong> {@code apply(I)} must only perform
 *       side effects that are necessary to implement the protected operation. Persistence,
 *       logging, and metrics within the use-case scope are acceptable. Do not bypass the
 *       use-case to invoke other aggregates, send notifications, or mutate unrelated state.</li>
 *   <li><strong>Timing:</strong> {@code apply(I)} is invoked <strong>after</strong>
 *       {@link FlagAwareExecutor#execute(FlagEnum, Object)} has resolved the effective flag.
 *       The effective flag is the requested flag if {@code FlagManager.isEnabled(flag)}
 *       returns {@code true}; otherwise it is {@link br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum#NONE}.</li>
 *   <li><strong>Statelessness:</strong> Implementations must be stateless or thread-safe.
 *       The registry caches one instance per flag and may invoke {@code apply} concurrently
 *       from different threads.</li>
 *   <li><strong>NONE is reserved:</strong> {@link br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum#NONE}
 *       is reserved for the default strategy (registered via
 *       {@link FlagStrategyRegistry.Builder#withDefault(FlagStrategy)}). It cannot be
 *       registered directly via {@link FlagStrategyRegistry.Builder#register(FlagEnum, FlagStrategy)}.</li>
 * </ul>
 *
 * <h2>Precedence</h2>
 * <p>Flag precedence rules (when a domain entity holds a flag) are documented in
 * {@link br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum}. Higher-specificity
 * flags win over {@code NONE}.</p>
 *
 * @param <I> use-case input type
 * @param <O> use-case output type
 * @see FlagAwareExecutor
 * @see FlagStrategyRegistry
 * @see br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum
 */
public interface FlagStrategy<I, O> {
    /**
     * Applies this strategy to the given input and returns the result.
     *
     * <p>This method is invoked by {@link FlagAwareExecutor#execute(FlagEnum, Object)}
     * after the effective flag has been resolved. The implementation is responsible for
     * executing the use case logic corresponding to this strategy's flag variant.</p>
     *
     * <p>Contract: the strategy's decision branch (which policy is applied, which validation
     * is used) must be deterministic, and side effects must be limited to the protected
     * operation's scope. Domain-generated identity and time-dependent sources produce
     * observable non-determinism but are acceptable. See the class-level documentation
     * for complete contract details.</p>
     *
     * @param input the use-case input
     * @return the use-case output
     */
    O apply(I input);
}
