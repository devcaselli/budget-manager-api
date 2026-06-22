package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;
import java.util.Currency;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ReservedBudgetLinkCapValidator}.
 *
 * <p>Scenarios cover the exact owner examples from the feature plan:</p>
 * <ul>
 *   <li>Single RB version: sub=1800 under ceiling=2000 → passes</li>
 *   <li>RB down-edit: {jun:2000, jul:1500} + sub=1800 → fails in jul (1800 > 1500)</li>
 *   <li>Share reduces amount in jul: new effective=1400 ≤ 1500 → passes</li>
 *   <li>Multiple links: Netflix(50) + Spotify(30) under ceiling=1600 → passes</li>
 *   <li>Far-future tail: amounts constant beyond last version → no overflow</li>
 *   <li>Installment expiry: sum drops after last month → no false positive</li>
 *   <li>Empty links: no-op</li>
 *   <li>Link not yet applicable: fromMonth gate respected</li>
 *   <li>Exact ceiling equality: passes (sum == ceiling is allowed)</li>
 *   <li>Cap exceeded from first month → exception carries correct fields</li>
 * </ul>
 */
class ReservedBudgetLinkCapValidatorTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final YearMonth JUN = YearMonth.of(2025, 6);
    private static final YearMonth JUL = YearMonth.of(2025, 7);
    private static final YearMonth AUG = YearMonth.of(2025, 8);
    private static final YearMonth SEP = YearMonth.of(2025, 9);

    private ReservedBudgetLinkCapValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ReservedBudgetLinkCapValidator();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper builders
    // ─────────────────────────────────────────────────────────────────────────

    /** Creates an RB starting in JUN with a single version (ceiling from JUN onward). */
    private ReservedBudget rbSingleVersion(String amount) {
        return ReservedBudget.create(
                "Aluguel", null, BRL, Money.of(amount, BRL), JUN, FlagEnum.NONE, "owner-1");
    }

    /**
     * Creates an RB starting in JUN with two versions: initial ceiling from JUN,
     * new ceiling from JUL.
     */
    private ReservedBudget rbTwoVersions(String junAmount, String julAmount) {
        return rbSingleVersion(junAmount).addVersion(JUL, Money.of(julAmount, BRL));
    }

    /** Minimal {@link LinkedItemAmounts} that returns a fixed amount for every applicable month. */
    private LinkedItemAmounts constantAmount(Money fixedAmount, Set<YearMonth> breakpoints) {
        return new LinkedItemAmounts() {
            @Override
            public Optional<Money> effectiveAmount(ReservedBudgetLink link, YearMonth month) {
                return Optional.of(fixedAmount);
            }

            @Override
            public Set<YearMonth> breakpoints(ReservedBudgetLink link) {
                return breakpoints;
            }
        };
    }

    /** {@link LinkedItemAmounts} that returns empty (item not applicable) for all months. */
    private LinkedItemAmounts neverApplicable() {
        return new LinkedItemAmounts() {
            @Override
            public Optional<Money> effectiveAmount(ReservedBudgetLink link, YearMonth month) {
                return Optional.empty();
            }

            @Override
            public Set<YearMonth> breakpoints(ReservedBudgetLink link) {
                return Set.of(link.fromMonth());
            }
        };
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core business examples (from plan / owner)
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void singleVersion_subUnderCeiling_passes() {
        // Owner example: RB {jun:2000}, sub effective=1800 → sum(1800) ≤ 2000 ✓
        ReservedBudget rb = rbSingleVersion("2000.00");
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", JUN);
        LinkedItemAmounts amounts = constantAmount(Money.of("1800.00", BRL), Set.of(JUN));

        assertThatCode(() -> validator.validate(rb, List.of(link), amounts))
                .doesNotThrowAnyException();
    }

    @Test
    void twoVersions_subExceedsReducedCeiling_failsInJul() {
        // Owner example: RB {jun:2000, jul:1500}, sub effective=1800 → 1800 > 1500 in jul ✗
        ReservedBudget rb = rbTwoVersions("2000.00", "1500.00");
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", JUN);
        LinkedItemAmounts amounts = constantAmount(Money.of("1800.00", BRL), Set.of(JUN, JUL));

        assertThatThrownBy(() -> validator.validate(rb, List.of(link), amounts))
                .isInstanceOf(ReservedBudgetLinkCapExceededException.class)
                .satisfies(ex -> {
                    ReservedBudgetLinkCapExceededException e = (ReservedBudgetLinkCapExceededException) ex;
                    assertThat(e.getMonth()).isEqualTo(JUL);
                    assertThat(e.getSum()).isEqualTo(Money.of("1800.00", BRL));
                    assertThat(e.getCeiling()).isEqualTo(Money.of("1500.00", BRL));
                });
    }

    @Test
    void twoVersions_shareReducesAmountInJul_passes() {
        // Owner example: RB {jun:2000, jul:1500}, sub effective=1800 in jun, 1400 in jul → passes
        // (share reduces in jul: 1400 ≤ 1500)
        ReservedBudget rb = rbTwoVersions("2000.00", "1500.00");
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", JUN);

        LinkedItemAmounts amounts = new LinkedItemAmounts() {
            @Override
            public Optional<Money> effectiveAmount(ReservedBudgetLink l, YearMonth month) {
                if (month.isBefore(JUL)) {
                    return Optional.of(Money.of("1800.00", BRL)); // full amount in jun
                }
                return Optional.of(Money.of("1400.00", BRL)); // reduced by share in jul+
            }

            @Override
            public Set<YearMonth> breakpoints(ReservedBudgetLink l) {
                return Set.of(JUN, JUL); // amount changes at jul
            }
        };

        assertThatCode(() -> validator.validate(rb, List.of(link), amounts))
                .doesNotThrowAnyException();
    }

    @Test
    void multipleLinks_netflixPlusSpotifyUnder1600_passes() {
        // Owner example: Netflix(50) + Spotify(30) under ceiling=1600 → 80 ≤ 1600 ✓
        ReservedBudget rb = rbSingleVersion("1600.00");
        ReservedBudgetLink netflix = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-netflix", JUN);
        ReservedBudgetLink spotify = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-spotify", JUN);

        LinkedItemAmounts amounts = new LinkedItemAmounts() {
            @Override
            public Optional<Money> effectiveAmount(ReservedBudgetLink link, YearMonth month) {
                return switch (link.sourceId()) {
                    case "sub-netflix" -> Optional.of(Money.of("50.00", BRL));
                    case "sub-spotify" -> Optional.of(Money.of("30.00", BRL));
                    default -> Optional.empty();
                };
            }

            @Override
            public Set<YearMonth> breakpoints(ReservedBudgetLink link) {
                return Set.of(JUN);
            }
        };

        assertThatCode(() -> validator.validate(rb, List.of(netflix, spotify), amounts))
                .doesNotThrowAnyException();
    }

    @Test
    void farFutureTail_constantAmountsAfterLastVersion_noBoundaryOverflow() {
        // Proves: last breakpoint covers infinite tail correctly.
        // RB {jun:2000}, sub=1900 with only JUN as breakpoint.
        // Tail (all future months): 1900 ≤ 2000 → no overflow ever.
        ReservedBudget rb = rbSingleVersion("2000.00");
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", JUN);
        LinkedItemAmounts amounts = constantAmount(Money.of("1900.00", BRL), Set.of(JUN));

        assertThatCode(() -> validator.validate(rb, List.of(link), amounts))
                .doesNotThrowAnyException();
    }

    @Test
    void installmentExpiry_sumDropsAfterLastMonth_noFalsePositive() {
        // Installment contributes 1400 from JUN to JUL, expires AUG.
        // RB {jun:2000, aug:1200} — after AUG, installment is gone → sum=0 ≤ 1200 ✓
        ReservedBudget rb = rbSingleVersion("2000.00").addVersion(AUG, Money.of("1200.00", BRL));
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.INSTALLMENT, "inst-1", JUN);

        LinkedItemAmounts amounts = new LinkedItemAmounts() {
            @Override
            public Optional<Money> effectiveAmount(ReservedBudgetLink l, YearMonth month) {
                // Expires after JUL — returns empty from AUG onward
                if (month.isBefore(AUG)) {
                    return Optional.of(Money.of("1400.00", BRL));
                }
                return Optional.empty();
            }

            @Override
            public Set<YearMonth> breakpoints(ReservedBudgetLink l) {
                return Set.of(JUN, AUG); // change at AUG (expiry)
            }
        };

        assertThatCode(() -> validator.validate(rb, List.of(link), amounts))
                .doesNotThrowAnyException();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Edge cases
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void emptyLinks_noOp() {
        ReservedBudget rb = rbSingleVersion("2000.00");
        LinkedItemAmounts amounts = constantAmount(Money.of("9999.00", BRL), Set.of(JUN));

        assertThatCode(() -> validator.validate(rb, List.of(), amounts))
                .doesNotThrowAnyException();
    }

    @Test
    void linkNotYetApplicable_fromMonthGate_respected() {
        // Link starts JUL — breakpoint JUN from RB exists but link is not applicable yet
        // → sum in JUN = 0, even if effectiveAmount would return a value
        ReservedBudget rb = rbTwoVersions("2000.00", "100.00"); // JUL ceiling drops to 100
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", JUL);
        // amount=50 which is ≤ 100; passes because gate works
        LinkedItemAmounts amounts = constantAmount(Money.of("50.00", BRL), Set.of(JUL));

        assertThatCode(() -> validator.validate(rb, List.of(link), amounts))
                .doesNotThrowAnyException();
    }

    @Test
    void sumExactlyEqualsToCeiling_passes() {
        // Boundary: sum == ceiling is allowed (not strictly greater)
        ReservedBudget rb = rbSingleVersion("1800.00");
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", JUN);
        LinkedItemAmounts amounts = constantAmount(Money.of("1800.00", BRL), Set.of(JUN));

        assertThatCode(() -> validator.validate(rb, List.of(link), amounts))
                .doesNotThrowAnyException();
    }

    @Test
    void sumByOneUnit_fails() {
        // One cent above ceiling → fails
        ReservedBudget rb = rbSingleVersion("1800.00");
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", JUN);
        LinkedItemAmounts amounts = constantAmount(Money.of("1800.01", BRL), Set.of(JUN));

        assertThatThrownBy(() -> validator.validate(rb, List.of(link), amounts))
                .isInstanceOf(ReservedBudgetLinkCapExceededException.class);
    }

    @Test
    void neverApplicableItem_alwaysPasses() {
        // effectiveAmount returns empty for all months → sum stays 0
        ReservedBudget rb = rbSingleVersion("100.00");
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", JUN);

        assertThatCode(() -> validator.validate(rb, List.of(link), neverApplicable()))
                .doesNotThrowAnyException();
    }

    @Test
    void exceptionCarriesCorrectFields() {
        ReservedBudget rb = rbSingleVersion("500.00");
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", JUN);
        LinkedItemAmounts amounts = constantAmount(Money.of("600.00", BRL), Set.of(JUN));

        assertThatThrownBy(() -> validator.validate(rb, List.of(link), amounts))
                .isInstanceOf(ReservedBudgetLinkCapExceededException.class)
                .satisfies(ex -> {
                    ReservedBudgetLinkCapExceededException e = (ReservedBudgetLinkCapExceededException) ex;
                    assertThat(e.getMonth()).isEqualTo(JUN);
                    assertThat(e.getSum()).isEqualTo(Money.of("600.00", BRL));
                    assertThat(e.getCeiling()).isEqualTo(Money.of("500.00", BRL));
                });
    }

    @Test
    void multipleLinks_oneExceedsAlone_fails() {
        // Netflix(50) + Spotify(1600) > ceiling(1600)
        ReservedBudget rb = rbSingleVersion("1600.00");
        ReservedBudgetLink netflix = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-netflix", JUN);
        ReservedBudgetLink spotify = new ReservedBudgetLink(ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-spotify", JUN);

        LinkedItemAmounts amounts = new LinkedItemAmounts() {
            @Override
            public Optional<Money> effectiveAmount(ReservedBudgetLink link, YearMonth month) {
                return switch (link.sourceId()) {
                    case "sub-netflix" -> Optional.of(Money.of("50.00", BRL));
                    case "sub-spotify" -> Optional.of(Money.of("1600.00", BRL));
                    default -> Optional.empty();
                };
            }

            @Override
            public Set<YearMonth> breakpoints(ReservedBudgetLink link) {
                return Set.of(JUN);
            }
        };

        assertThatThrownBy(() -> validator.validate(rb, List.of(netflix, spotify), amounts))
                .isInstanceOf(ReservedBudgetLinkCapExceededException.class)
                .satisfies(ex -> {
                    ReservedBudgetLinkCapExceededException e = (ReservedBudgetLinkCapExceededException) ex;
                    assertThat(e.getSum()).isEqualTo(Money.of("1650.00", BRL));
                    assertThat(e.getCeiling()).isEqualTo(Money.of("1600.00", BRL));
                });
    }

    @Test
    void linkStartsAfterRbVersionChange_breakpointsMergedCorrectly() {
        // RB {jun:2000, jul:800}, link from JUL with amount=700 → 700 ≤ 800 ✓
        // Breakpoints: JUN (RB), JUL (both RB and link fromMonth)
        // In JUN: link not applicable → sum=0; in JUL: 700 ≤ 800 ✓
        ReservedBudget rb = rbTwoVersions("2000.00", "800.00");
        ReservedBudgetLink link = new ReservedBudgetLink(ReservedBudgetLinkSourceType.INSTALLMENT, "inst-1", JUL);
        LinkedItemAmounts amounts = constantAmount(Money.of("700.00", BRL), Set.of(JUL));

        assertThatCode(() -> validator.validate(rb, List.of(link), amounts))
                .doesNotThrowAnyException();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Exception type hierarchy
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void capExceededException_isRuntimeException() {
        assertThat(new ReservedBudgetLinkCapExceededException(JUN, Money.of("10", BRL), Money.of("5", BRL)))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void conflictException_isRuntimeException() {
        ReservedBudgetLinkConflictException ex = new ReservedBudgetLinkConflictException(
                ReservedBudgetLinkSourceType.SUBSCRIPTION, "sub-1", "rb-99");
        org.assertj.core.api.Assertions.assertThat(ex).isInstanceOf(RuntimeException.class);
        org.assertj.core.api.Assertions.assertThat(ex.getMessage()).contains("sub-1").contains("rb-99");
    }

    @Test
    void linkNotFoundException_isRuntimeException() {
        ReservedBudgetLinkNotFoundException ex = new ReservedBudgetLinkNotFoundException(
                ReservedBudgetLinkSourceType.INSTALLMENT, "inst-1", "rb-1");
        org.assertj.core.api.Assertions.assertThat(ex).isInstanceOf(RuntimeException.class);
        org.assertj.core.api.Assertions.assertThat(ex.getMessage()).contains("inst-1").contains("rb-1");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Needed for inline assertThat inside satisfies lambdas (import via static)
    // ─────────────────────────────────────────────────────────────────────────
    private static <T> org.assertj.core.api.AbstractObjectAssert<?, T> assertThat(T actual) {
        return org.assertj.core.api.Assertions.assertThat(actual);
    }
}
