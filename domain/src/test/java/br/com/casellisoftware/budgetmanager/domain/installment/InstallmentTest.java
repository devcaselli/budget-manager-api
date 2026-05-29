package br.com.casellisoftware.budgetmanager.domain.installment;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InstallmentTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void create_happyPath_buildsConsistentInstance() {
        Installment installment = Installment.create(
                "Notebook",
                Money.of(new BigDecimal("6000.00")),
                Money.of(new BigDecimal("1000.00")),
                6,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "w1",
                YearMonth.of(2026, 5),
                FlagEnum.NONE
        );

        assertThat(installment.getId()).isNotBlank();
        assertThat(installment.getLastInstallmentDate()).isEqualTo(YearMonth.of(2026, 10));
        assertThat(installment.isDeleted()).isFalse();
        assertThat(installment.getDeletedAt()).isNull();
    }

    @Test
    void create_installmentNumberBelowTwo_throws() {
        assertThatThrownBy(() -> Installment.create(
                "x",
                Money.of("100"),
                Money.of("100"),
                1,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "w1",
                YearMonth.of(2026, 5),
                FlagEnum.NONE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(">= 2");
    }

    @Test
    void create_residualWithinTolerance_isAccepted() {
        Installment installment = Installment.create(
                "Lunch shared",
                Money.of(new BigDecimal("100.00")),
                Money.of(new BigDecimal("33.33")),
                3,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "w1",
                YearMonth.of(2026, 5),
                FlagEnum.NONE
        );

        assertThat(installment.getInstallmentValue().amount()).isEqualByComparingTo("33.33");
    }

    @Test
    void create_residualExceedsTolerance_throws() {
        assertThatThrownBy(() -> Installment.create(
                "Bad split",
                Money.of(new BigDecimal("100.00")),
                Money.of(new BigDecimal("30.00")),
                3,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "w1",
                YearMonth.of(2026, 5),
                FlagEnum.NONE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tolerance");
    }

    @Test
    void create_currencyMismatch_throws() {
        assertThatThrownBy(() -> Installment.create(
                "x",
                Money.of(new BigDecimal("100.00")),
                Money.of(new BigDecimal("50.00"), Currency.getInstance("USD")),
                2,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "w1",
                YearMonth.of(2026, 5),
                FlagEnum.NONE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currency must match");
    }

    @Test
    void delete_setsDeletedAndDeletedAt() {
        Installment installment = sampleInstallment();
        Installment deleted = installment.delete(FIXED_CLOCK);

        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.getDeletedAt()).isEqualTo(LocalDate.of(2026, 6, 15).atTime(10, 0));
        assertThat(installment.isDeleted()).isFalse();
    }

    @Test
    void delete_alreadyDeleted_throws() {
        Installment installment = sampleInstallment();
        Installment deleted = installment.delete(FIXED_CLOCK);

        assertThatThrownBy(() -> deleted.delete(FIXED_CLOCK))
                .isInstanceOf(InstallmentAlreadyDeletedException.class);
    }

    @Test
    void rebuild_withInconsistentDeletedAt_throws() {
        assertThatThrownBy(() -> Installment.rebuild(
                "id",
                "x",
                Money.of("100"),
                Money.of("50"),
                2,
                LocalDate.of(2026, 5, 10),
                YearMonth.of(2026, 6),
                "cc1",
                "w1",
                YearMonth.of(2026, 5),
                true,
                null,
                FlagEnum.NONE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deletedAt must not be null");
    }

    private static Installment sampleInstallment() {
        return Installment.create(
                "Notebook",
                Money.of(new BigDecimal("6000.00")),
                Money.of(new BigDecimal("1000.00")),
                6,
                LocalDate.of(2026, 5, 10),
                "cc1",
                "w1",
                YearMonth.of(2026, 5),
                FlagEnum.NONE
        );
    }
}
