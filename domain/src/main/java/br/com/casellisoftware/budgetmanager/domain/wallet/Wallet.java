package br.com.casellisoftware.budgetmanager.domain.wallet;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagAware;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.IllegalWalletStateTransitionException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing a wallet (budget envelope).
 *
 * <p>Immutable: every state-changing operation returns a new instance.
 * Reconstruction from persistence uses the public constructor directly
 * (e.g. via MapStruct).</p>
 *
 * <p>{@code remaining} represents the portion of {@code budget} that has not
 * yet been consumed by expenses. A newly created wallet starts with
 * {@code remaining == budget}; as expenses are debited, {@code remaining}
 * shrinks toward zero via {@link #debit(Money)}.</p>
 *
 * <p>{@code effectiveMonth} is the canonical month this wallet represents and
 * is the implicit link used by subscription-charge backfill — there is no FK
 * from a {@code Subscription} to a {@code Wallet}; charges are routed by
 * matching {@code effectiveMonth}. {@code state} drives lifecycle/uniqueness
 * (see {@link WalletState}).</p>
 */
public final class Wallet implements FlagAware {

    public static final String LEGACY_OWNER_ID = "legacy";

    private final String id;
    private final String ownerId;
    private final String description;
    private final Money budget;
    private final Money remaining;
    private final LocalDate startDate;
    private final LocalDate closedDate;
    private final Boolean closed;
    private final YearMonth effectiveMonth;
    private final WalletState state;
    private final FlagEnum flag;

    public Wallet(String id,
                  String description,
                  Money budget,
                  Money remaining,
                  LocalDate startDate,
                  LocalDate closedDate,
                  Boolean closed,
                  YearMonth effectiveMonth,
                  WalletState state,
                  FlagEnum flag) {
        this(id, LEGACY_OWNER_ID, description, budget, remaining, startDate, closedDate, closed, effectiveMonth, state, flag);
    }

    public Wallet(String id,
                  String ownerId,
                  String description,
                  Money budget,
                  Money remaining,
                  LocalDate startDate,
                  LocalDate closedDate,
                  Boolean closed,
                  YearMonth effectiveMonth,
                  WalletState state,
                  FlagEnum flag) {
        this.id = id;
        this.ownerId = requireNonBlank(ownerId, "ownerId");
        this.description = description;
        this.budget = budget;
        this.remaining = remaining;
        this.startDate = startDate;
        this.closedDate = closedDate;
        this.closed = closed == null ? Boolean.FALSE : closed;
        this.effectiveMonth = effectiveMonth;
        this.state = state;
        this.flag = flag == null ? FlagEnum.NONE : flag;
    }

    public static Wallet create(String description,
                                Money budget,
                                LocalDate closedDate,
                                LocalDate startDate,
                                Boolean closed,
                                YearMonth effectiveMonth,
                                WalletState state,
                                FlagEnum flag) {
        return create(description, budget, closedDate, startDate, closed, effectiveMonth, state, flag, LEGACY_OWNER_ID);
    }

    public static Wallet create(String description,
                                Money budget,
                                LocalDate closedDate,
                                LocalDate startDate,
                                Boolean closed,
                                YearMonth effectiveMonth,
                                WalletState state,
                                FlagEnum flag,
                                String ownerId) {
        Objects.requireNonNull(effectiveMonth, "effectiveMonth must not be null");
        Objects.requireNonNull(state, "state must not be null");
        return new Wallet(UUID.randomUUID().toString(), ownerId, description, budget, budget,
                startDate, closedDate, closed, effectiveMonth, state, flag);
    }

    /**
     * Returns a new {@code Wallet} with {@code remaining} reduced by {@code amount}.
     */
    public Wallet debit(Money amount) {
        Money newRemaining = this.remaining.debitBy(amount);
        return new Wallet(this.id, this.ownerId, this.description, this.budget, newRemaining,
                this.startDate, this.closedDate, this.closed, this.effectiveMonth, this.state, this.flag);
    }

    /**
     * Returns a new {@code Wallet} with {@code remaining} increased by {@code amount}.
     */
    public Wallet credit(Money amount) {
        Money newRemaining = this.remaining.add(amount);
        if (newRemaining.isGreaterThan(this.budget)) {
            throw new IllegalStateException("credit would push remaining above budget: id=" + this.id);
        }
        return new Wallet(this.id, this.ownerId, this.description, this.budget, newRemaining,
                this.startDate, this.closedDate, this.closed, this.effectiveMonth, this.state, this.flag);
    }

    /**
     * Applies an explicit partial update. Financial state derived from debits
     * ({@code remaining}) and lifecycle identity ({@code startDate}) are not patchable.
     */
    public Wallet patch(WalletPatch patch) {
        Objects.requireNonNull(patch, "patch must not be null");
        if (patch.isEmpty()) {
            return this;
        }

        String patchedDescription = patch.description().orElse(this.description);
        Money patchedBudget = patch.budget().orElse(this.budget);
        LocalDate patchedClosedDate = patch.closedDate().orElse(this.closedDate);
        Boolean patchedClosed = patch.closed().orElse(this.closed);
        FlagEnum patchedFlag = patch.flag().orElse(this.flag);

        validatePatchedState(patchedBudget);

        WalletState patchedState = patch.state().orElse(this.state);

        Wallet base = this;
        if (patchedState != this.state) {
            base = base.transitionTo(patchedState);
        }

        if (Objects.equals(base.description, patchedDescription)
                && Objects.equals(base.budget, patchedBudget)
                && Objects.equals(base.closedDate, patchedClosedDate)
                && Objects.equals(base.closed, patchedClosed)
                && Objects.equals(base.state, patchedState)
                && Objects.equals(base.flag, patchedFlag)) {
            return base;
        }

        return new Wallet(base.id, base.ownerId, patchedDescription, patchedBudget, base.remaining,
                base.startDate, patchedClosedDate, patchedClosed, base.effectiveMonth, patchedState, patchedFlag);
    }

    private void validatePatchedState(Money budget) {
        Objects.requireNonNull(budget, "budget must not be null");
        if (this.remaining != null && this.remaining.isGreaterThan(budget)) {
            throw new IllegalArgumentException("remaining must not exceed budget");
        }
    }

    /**
     * Returns whether the wallet is closed by either the explicit flag or by
     * {@code closedDate} having passed (today &gt;= closedDate).
     */
    public boolean isClosed(LocalDate today) {
        Objects.requireNonNull(today, "today must not be null");
        if (Boolean.TRUE.equals(this.closed)) {
            return true;
        }
        return this.closedDate != null && !today.isBefore(this.closedDate);
    }

    /**
     * Returns whether the wallet is closed using the explicit flag only. For
     * date-based evaluation prefer {@link #isClosed(LocalDate)}.
     */
    public boolean isClosed() {
        return Boolean.TRUE.equals(this.closed);
    }

    /**
     * Returns a new {@code Wallet} transitioned to {@code target} state, applying
     * the lifecycle rules:
     * <ul>
     *   <li>PRODUCTION → REVIEW: requires {@code closed == true}</li>
     *   <li>PRODUCTION → PREVIEW: forbidden</li>
     *   <li>PREVIEW → PRODUCTION: allowed (caller must check uniqueness)</li>
     *   <li>PREVIEW → REVIEW: forbidden</li>
     *   <li>REVIEW → *: forbidden (terminal)</li>
     * </ul>
     */
    public Wallet transitionTo(WalletState target) {
        Objects.requireNonNull(target, "target must not be null");
        if (this.state == target) {
            return this;
        }
        if (this.state == WalletState.REVIEW) {
            throw new IllegalWalletStateTransitionException(this.state, target, "REVIEW is terminal");
        }
        switch (this.state) {
            case PRODUCTION -> {
                if (target == WalletState.REVIEW) {
                    if (!Boolean.TRUE.equals(this.closed)) {
                        throw new IllegalWalletStateTransitionException(this.state, target,
                                "wallet must be closed before moving to REVIEW");
                    }
                } else if (target == WalletState.PREVIEW) {
                    throw new IllegalWalletStateTransitionException(this.state, target,
                            "PRODUCTION cannot demote to PREVIEW");
                }
            }
            case PREVIEW -> {
                if (target == WalletState.REVIEW) {
                    throw new IllegalWalletStateTransitionException(this.state, target,
                            "PREVIEW cannot transition to REVIEW directly");
                }
                // PREVIEW -> PRODUCTION allowed; uniqueness validated by caller policy.
            }
            default -> { /* unreachable */ }
        }
        return new Wallet(this.id, this.ownerId, this.description, this.budget, this.remaining,
                this.startDate, this.closedDate, this.closed, this.effectiveMonth, target, this.flag);
    }

    public String getId() {
        return id;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public String getDescription() {
        return description;
    }

    public Money getBudget() {
        return budget;
    }

    public Money getRemaining() {
        return remaining;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getClosedDate() {
        return closedDate;
    }

    public Boolean getClosed() {
        return closed;
    }

    public YearMonth getEffectiveMonth() {
        return effectiveMonth;
    }

    public WalletState getState() {
        return state;
    }

    @Override
    public FlagEnum getFlag() {
        return flag;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Wallet other)) return false;
        return Objects.equals(id, other.id)
                && Objects.equals(ownerId, other.ownerId)
                && Objects.equals(description, other.description)
                && Objects.equals(budget, other.budget)
                && Objects.equals(remaining, other.remaining)
                && Objects.equals(startDate, other.startDate)
                && Objects.equals(closedDate, other.closedDate)
                && Objects.equals(closed, other.closed)
                && Objects.equals(effectiveMonth, other.effectiveMonth)
                && Objects.equals(state, other.state)
                && Objects.equals(flag, other.flag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ownerId, description, budget, remaining, startDate, closedDate, closed, effectiveMonth, state, flag);
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
