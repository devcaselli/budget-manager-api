package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;
import java.util.Optional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservedBudgetTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");
    private static final YearMonth MARCH = YearMonth.of(2025, 3);
    private static final YearMonth AUGUST = YearMonth.of(2025, 8);

    private static ReservedBudget aluguel() {
        return ReservedBudget.create(
                "Aluguel",
                "monthly rent reservation",
                BRL,
                Money.of("2000.00", BRL),
                MARCH,
                FlagEnum.NONE,
                "owner-1"
        );
    }

    @Test
    void create_initializesWithSingleVersionAtStartMonth() {
        ReservedBudget rb = aluguel();

        assertThat(rb.getId()).isNotBlank();
        assertThat(rb.getDescription()).isEqualTo("Aluguel");
        assertThat(rb.getDetails()).isEqualTo("monthly rent reservation");
        assertThat(rb.getCurrency()).isEqualTo(BRL);
        assertThat(rb.getStartMonth()).isEqualTo(MARCH);
        assertThat(rb.isDeleted()).isFalse();
        assertThat(rb.getDeletedAt()).isNull();
        assertThat(rb.getVersions()).hasSize(1);
        assertThat(rb.getVersions().getFirst().effectiveMonth()).isEqualTo(MARCH);
        assertThat(rb.getVersions().getFirst().amount()).isEqualTo(Money.of("2000.00", BRL));
    }

    @Test
    void create_blankDetailsBecomesNull() {
        ReservedBudget rb = ReservedBudget.create("Aluguel", "   ", BRL, Money.of("2000.00", BRL), MARCH, FlagEnum.NONE, "owner-1");
        assertThat(rb.getDetails()).isNull();
    }

    @Test
    void resolveAmount_returnsEffectiveAmountForMonth() {
        // 2000 from March; 1500 from August onward
        ReservedBudget rb = aluguel().addVersion(AUGUST, Money.of("1500.00", BRL));

        assertThat(rb.resolveAmount(MARCH)).isEqualTo(Money.of("2000.00", BRL));
        assertThat(rb.resolveAmount(YearMonth.of(2025, 5))).isEqualTo(Money.of("2000.00", BRL));
        assertThat(rb.resolveAmount(YearMonth.of(2025, 7))).isEqualTo(Money.of("2000.00", BRL));
        assertThat(rb.resolveAmount(AUGUST)).isEqualTo(Money.of("1500.00", BRL));
        assertThat(rb.resolveAmount(YearMonth.of(2025, 10))).isEqualTo(Money.of("1500.00", BRL));
    }

    @Test
    void resolveAmount_beforeStartMonth_throws() {
        assertThatThrownBy(() -> aluguel().resolveAmount(YearMonth.of(2025, 2)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no reserved-budget version");
    }

    @Test
    void addVersion_keepsVersionsSortedAndDeduplicatedByMonth() {
        ReservedBudget rb = aluguel()
                .addVersion(AUGUST, Money.of("1500.00", BRL))
                .addVersion(YearMonth.of(2025, 5), Money.of("1800.00", BRL))
                // re-set August to a new amount (replaces, not duplicates)
                .addVersion(AUGUST, Money.of("1400.00", BRL));

        assertThat(rb.getVersions()).hasSize(3);
        assertThat(rb.getVersions().stream().map(v -> v.effectiveMonth()).toList())
                .containsExactly(MARCH, YearMonth.of(2025, 5), AUGUST);
        assertThat(rb.resolveAmount(AUGUST)).isEqualTo(Money.of("1400.00", BRL));
        assertThat(rb.resolveAmount(YearMonth.of(2025, 5))).isEqualTo(Money.of("1800.00", BRL));
    }

    @Test
    void addVersion_sameMonthSameAmount_returnsSameInstance() {
        ReservedBudget rb = aluguel();
        ReservedBudget same = rb.addVersion(MARCH, Money.of("2000.00", BRL));
        assertThat(same).isSameAs(rb);
    }

    @Test
    void addVersion_beforeStartMonth_throws() {
        assertThatThrownBy(() -> aluguel().addVersion(YearMonth.of(2025, 2), Money.of("100.00", BRL)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startMonth");
    }

    @Test
    void addVersion_currencyMismatch_throws() {
        assertThatThrownBy(() -> aluguel().addVersion(AUGUST, Money.of("1500.00", USD)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency");
    }

    @Test
    void isApplicable_trueFromStartMonthOnward_noEnd() {
        ReservedBudget rb = aluguel();
        assertThat(rb.isApplicable(YearMonth.of(2025, 2))).isFalse();
        assertThat(rb.isApplicable(MARCH)).isTrue();
        assertThat(rb.isApplicable(AUGUST)).isTrue();
        assertThat(rb.isApplicable(YearMonth.of(2030, 1))).isTrue();
    }

    @Test
    void applyPatch_changingAmount_addsVersionFromCurrentMonth() {
        ReservedBudget rb = aluguel();
        ReservedBudgetPatch patch = ReservedBudgetPatch.empty().withNewAmount(Money.of("1500.00", BRL));

        ReservedBudget patched = rb.applyPatch(patch, AUGUST);

        assertThat(patched.resolveAmount(YearMonth.of(2025, 7))).isEqualTo(Money.of("2000.00", BRL));
        assertThat(patched.resolveAmount(AUGUST)).isEqualTo(Money.of("1500.00", BRL));
    }

    @Test
    void applyPatch_sameAmount_doesNotAddVersion() {
        ReservedBudget rb = aluguel();
        ReservedBudget patched = rb.applyPatch(ReservedBudgetPatch.empty().withNewAmount(Money.of("2000.00", BRL)), AUGUST);
        assertThat(patched.getVersions()).hasSize(1);
    }

    @Test
    void applyPatch_descriptionAndDetails() {
        ReservedBudget rb = aluguel()
                .applyPatch(ReservedBudgetPatch.empty().withDescription("Aluguel apto").withDetails("new note"), MARCH);
        assertThat(rb.getDescription()).isEqualTo("Aluguel apto");
        assertThat(rb.getDetails()).isEqualTo("new note");
    }

    @Test
    void markDeleted_setsDeletedAndIsIdempotent() {
        LocalDateTime now = LocalDateTime.of(2025, 9, 1, 10, 0);
        ReservedBudget deleted = aluguel().markDeleted(now);

        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.getDeletedAt()).isEqualTo(now);

        ReservedBudget again = deleted.markDeleted(LocalDateTime.of(2025, 10, 1, 0, 0));
        assertThat(again).isSameAs(deleted);
        assertThat(again.getDeletedAt()).isEqualTo(now);
    }

    @Test
    void rebuild_restoresAllFields() {
        ReservedBudget rb = ReservedBudget.rebuild(
                "rb-1",
                "owner-1",
                "Aluguel",
                "note",
                BRL,
                MARCH,
                List.of(
                        new ReservedBudgetVersion(MARCH, Money.of("2000.00", BRL)),
                        new ReservedBudgetVersion(AUGUST, Money.of("1500.00", BRL))
                ),
                List.of(),
                false,
                null,
                FlagEnum.NONE
        );

        assertThat(rb.getId()).isEqualTo("rb-1");
        assertThat(rb.resolveAmount(AUGUST)).isEqualTo(Money.of("1500.00", BRL));
    }

    // ── ReservedBudgetLink ──────────────────────────────────────────────────

    @Test
    void link_isApplicable_trueFromFromMonthOnward() {
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", MARCH);
        assertThat(link.isApplicable(YearMonth.of(2025, 2))).isFalse();
        assertThat(link.isApplicable(MARCH)).isTrue();
        assertThat(link.isApplicable(AUGUST)).isTrue();
    }

    @Test
    void link_nullSourceType_throws() {
        assertThatThrownBy(() -> new ReservedBudgetLink(null, "sub-1", MARCH))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void link_blankSourceId_throws() {
        assertThatThrownBy(() -> new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, " ", MARCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceId");
    }

    @Test
    void link_nullFromMonth_throws() {
        assertThatThrownBy(() -> new ReservedBudgetLink(ReservedBudgetLinkSourceType.INSTALLMENT, "inst-1", null))
                .isInstanceOf(NullPointerException.class);
    }

    // ── addLink / removeLink / findLink ────────────────────────────────────

    @Test
    void addLink_addsLinkToAggregate() {
        ReservedBudget rb = aluguel();
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", MARCH);

        ReservedBudget withLink = rb.addLink(link);

        assertThat(withLink.getLinks()).hasSize(1);
        assertThat(withLink.getLinks().getFirst()).isEqualTo(link);
    }

    @Test
    void addLink_sameKey_replacesExisting() {
        ReservedBudgetLink linkMarch = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", MARCH);
        ReservedBudgetLink linkAugust = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", AUGUST);

        ReservedBudget rb = aluguel().addLink(linkMarch).addLink(linkAugust);

        assertThat(rb.getLinks()).hasSize(1);
        assertThat(rb.getLinks().getFirst().fromMonth()).isEqualTo(AUGUST);
    }

    @Test
    void addLink_differentKeys_bothPresent() {
        ReservedBudget rb = aluguel()
                .addLink(new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", MARCH))
                .addLink(new ReservedBudgetLink(ReservedBudgetLinkSourceType.INSTALLMENT, "inst-1", MARCH));

        assertThat(rb.getLinks()).hasSize(2);
    }

    @Test
    void addLink_fromMonthBeforeStartMonth_throws() {
        YearMonth beforeStart = YearMonth.of(2025, 2);
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", beforeStart);

        assertThatThrownBy(() -> aluguel().addLink(link))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("startMonth");
    }

    @Test
    void removeLink_removesMatchingLink() {
        ReservedBudget rb = aluguel()
                .addLink(new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", MARCH))
                .addLink(new ReservedBudgetLink(ReservedBudgetLinkSourceType.INSTALLMENT, "inst-1", MARCH));

        ReservedBudget removed = rb.removeLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1");

        assertThat(removed.getLinks()).hasSize(1);
        assertThat(removed.getLinks().getFirst().sourceId()).isEqualTo("inst-1");
    }

    @Test
    void removeLink_nonExistentLink_returnsSameInstance() {
        ReservedBudget rb = aluguel();
        ReservedBudget same = rb.removeLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "not-there");
        assertThat(same).isSameAs(rb);
    }

    @Test
    void findLink_returnsLinkIfPresent() {
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", MARCH);
        ReservedBudget rb = aluguel().addLink(link);

        assertThat(rb.findLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1")).contains(link);
        assertThat(rb.findLink(ReservedBudgetLinkSourceType.INSTALLMENT, "sub-1")).isEmpty();
        assertThat(rb.findLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "other")).isEmpty();
    }

    @Test
    void normalizeLinks_duplicateKey_throws() {
        ReservedBudgetLink l1 = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", MARCH);
        ReservedBudgetLink l2 = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", AUGUST);

        assertThatThrownBy(() -> ReservedBudget.rebuild(
                "rb-1", "owner-1", "Aluguel", null, BRL, MARCH,
                List.of(new ReservedBudgetVersion(MARCH, Money.of("2000.00", BRL))),
                List.of(l1, l2),
                false, null, FlagEnum.NONE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate");
    }

    @Test
    void links_preservedAcrossMutators() {
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", MARCH);
        ReservedBudget rb = aluguel().addLink(link);

        assertThat(rb.rename("Outro").getLinks()).containsExactly(link);
        assertThat(rb.withDetails("detail").getLinks()).containsExactly(link);
        assertThat(rb.addVersion(AUGUST, Money.of("1500.00", BRL)).getLinks()).containsExactly(link);
        assertThat(rb.markDeleted(LocalDateTime.now()).getLinks()).containsExactly(link);
    }

    @Test
    void create_initializesWithEmptyLinks() {
        assertThat(aluguel().getLinks()).isEmpty();
    }

    @Test
    void equalsAndHashCode_basedOnIdAndOwner() {
        ReservedBudget a = ReservedBudget.rebuild("rb-1", "owner-1", "A", null, BRL, MARCH,
                List.of(new ReservedBudgetVersion(MARCH, Money.of("2000.00", BRL))), List.of(), false, null, FlagEnum.NONE);
        ReservedBudget b = ReservedBudget.rebuild("rb-1", "owner-1", "Different desc", "x", BRL, MARCH,
                List.of(new ReservedBudgetVersion(MARCH, Money.of("9999.00", BRL))), List.of(), true, LocalDateTime.now(), FlagEnum.NONE);

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
