package br.com.casellisoftware.budgetmanager.domain.extrabudget;

import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExtraBudgetTest {

    private static final String OWNER_ID = "owner-1";
    private static final String DESCRIPTION = "bonus";
    private static final String WALLET_ID = "wallet-1";
    private static final String BULLET_A = "bullet-1";
    private static final String BULLET_B = "bullet-2";

    private static List<ExtraBudgetAllocation> allocations(Money a, Money b) {
        return List.of(
                new ExtraBudgetAllocation(BULLET_A, a),
                new ExtraBudgetAllocation(BULLET_B, b)
        );
    }

    @Test
    void create_validInput_returnsExtraBudget() {
        Money amount = Money.of("300.00");
        List<ExtraBudgetAllocation> allocs = allocations(Money.of("200.00"), Money.of("100.00"));

        ExtraBudget eb = ExtraBudget.create(OWNER_ID, DESCRIPTION, WALLET_ID, amount, allocs);

        assertThat(eb.getId()).isNotNull();
        assertThat(eb.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(eb.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(eb.getWalletId()).isEqualTo(WALLET_ID);
        assertThat(eb.getAmount()).isEqualTo(amount);
        assertThat(eb.getAllocations()).hasSize(2);
        assertThat(eb.isDeleted()).isFalse();
        assertThat(eb.getDeletedAt()).isNull();
    }

    @Test
    void create_blankDescription_throws() {
        Money amount = Money.of("300.00");
        List<ExtraBudgetAllocation> allocs = allocations(Money.of("200.00"), Money.of("100.00"));

        assertThatThrownBy(() -> ExtraBudget.create(OWNER_ID, "  ", WALLET_ID, amount, allocs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description");
    }

    @Test
    void create_blankOwnerId_throws() {
        Money amount = Money.of("300.00");
        List<ExtraBudgetAllocation> allocs = allocations(Money.of("200.00"), Money.of("100.00"));

        assertThatThrownBy(() -> ExtraBudget.create("", DESCRIPTION, WALLET_ID, amount, allocs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ownerId");
    }

    @Test
    void create_blankWalletId_throws() {
        Money amount = Money.of("300.00");
        List<ExtraBudgetAllocation> allocs = allocations(Money.of("200.00"), Money.of("100.00"));

        assertThatThrownBy(() -> ExtraBudget.create(OWNER_ID, DESCRIPTION, "  ", amount, allocs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("walletId");
    }

    @Test
    void create_nonPositiveAmount_throws() {
        assertThatThrownBy(() ->
                ExtraBudget.create(OWNER_ID, DESCRIPTION, WALLET_ID,
                        Money.of("0.00"),
                        List.of(new ExtraBudgetAllocation(BULLET_A, Money.of("0.00")))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be positive");
    }

    @Test
    void create_emptyAllocations_throws() {
        assertThatThrownBy(() ->
                ExtraBudget.create(OWNER_ID, DESCRIPTION, WALLET_ID, Money.of("100.00"), List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allocations must not be empty");
    }

    @Test
    void create_duplicateBulletIdInAllocations_throws() {
        List<ExtraBudgetAllocation> allocs = List.of(
                new ExtraBudgetAllocation(BULLET_A, Money.of("150.00")),
                new ExtraBudgetAllocation(BULLET_A, Money.of("150.00"))
        );

        assertThatThrownBy(() ->
                ExtraBudget.create(OWNER_ID, DESCRIPTION, WALLET_ID, Money.of("300.00"), allocs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate bulletId");
    }

    @Test
    void create_allocationWithNonPositiveAmount_throws() {
        List<ExtraBudgetAllocation> allocs = List.of(
                new ExtraBudgetAllocation(BULLET_A, Money.of("300.00")),
                new ExtraBudgetAllocation(BULLET_B, Money.of("0.00"))
        );

        assertThatThrownBy(() ->
                ExtraBudget.create(OWNER_ID, DESCRIPTION, WALLET_ID, Money.of("300.00"), allocs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("allocation amount must be positive");
    }

    @Test
    void create_sumOfAllocationsDifferentFromAmount_throws() {
        List<ExtraBudgetAllocation> allocs = allocations(Money.of("100.00"), Money.of("100.00"));

        assertThatThrownBy(() ->
                ExtraBudget.create(OWNER_ID, DESCRIPTION, WALLET_ID, Money.of("300.00"), allocs))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sum of allocations");
    }

    @Test
    void create_currencyMismatchBetweenAmountAndAllocation_throws() {
        Currency usd = Currency.getInstance("USD");
        List<ExtraBudgetAllocation> allocs = List.of(
                new ExtraBudgetAllocation(BULLET_A, Money.of(new java.math.BigDecimal("300.00"), usd))
        );

        // amount is BRL (default), allocation is USD → Money.add throws on currency mismatch
        assertThatThrownBy(() ->
                ExtraBudget.create(OWNER_ID, DESCRIPTION, WALLET_ID, Money.of("300.00"), allocs))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void markDeleted_setsDeletedTrueAndDeletedAt() {
        ExtraBudget eb = ExtraBudget.create(OWNER_ID, DESCRIPTION, WALLET_ID,
                Money.of("300.00"), allocations(Money.of("200.00"), Money.of("100.00")));
        LocalDateTime now = LocalDateTime.of(2026, 5, 18, 12, 0);

        ExtraBudget deleted = eb.markDeleted(now);

        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.getDeletedAt()).isEqualTo(now);
        assertThat(deleted.getId()).isEqualTo(eb.getId());
        // original unchanged
        assertThat(eb.isDeleted()).isFalse();
    }

    @Test
    void markDeleted_onAlreadyDeleted_isIdempotent() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 18, 12, 0);
        ExtraBudget eb = ExtraBudget.create(OWNER_ID, DESCRIPTION, WALLET_ID,
                Money.of("300.00"), allocations(Money.of("200.00"), Money.of("100.00")));
        ExtraBudget deleted = eb.markDeleted(now);

        ExtraBudget deletedAgain = deleted.markDeleted(LocalDateTime.now());

        assertThat(deletedAgain).isSameAs(deleted);
        assertThat(deletedAgain.getDeletedAt()).isEqualTo(now);
    }

    @Test
    void rebuild_preservesAllFields() {
        LocalDateTime deletedAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        List<ExtraBudgetAllocation> allocs = allocations(Money.of("200.00"), Money.of("100.00"));

        ExtraBudget eb = ExtraBudget.rebuild(
                "fixed-id", OWNER_ID, DESCRIPTION, WALLET_ID,
                Money.of("300.00"), allocs, true, deletedAt);

        assertThat(eb.getId()).isEqualTo("fixed-id");
        assertThat(eb.getOwnerId()).isEqualTo(OWNER_ID);
        assertThat(eb.getDescription()).isEqualTo(DESCRIPTION);
        assertThat(eb.getWalletId()).isEqualTo(WALLET_ID);
        assertThat(eb.getAmount()).isEqualTo(Money.of("300.00"));
        assertThat(eb.getAllocations()).hasSize(2);
        assertThat(eb.isDeleted()).isTrue();
        assertThat(eb.getDeletedAt()).isEqualTo(deletedAt);
    }
}
