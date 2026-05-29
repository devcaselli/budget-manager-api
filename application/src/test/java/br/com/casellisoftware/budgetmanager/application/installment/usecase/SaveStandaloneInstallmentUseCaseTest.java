package br.com.casellisoftware.budgetmanager.application.installment.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.SaveStandaloneInstallmentInput;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InvalidStandaloneInstallmentInputException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveStandaloneInstallmentUseCaseTest {

    // Fixed clock: 2025-06-01 — purchase dates in Jan/2025 are safely in the past
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2025-06-01T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private FindCreditCardByIdBoundary findCreditCardByIdBoundary;

    private SaveStandaloneInstallmentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new SaveStandaloneInstallmentUseCase(installmentRepository, findCreditCardByIdBoundary, FIXED_CLOCK);
    }

    @Test
    void execute_withOriginalValue_calculatesInstallmentValue() {
        SaveStandaloneInstallmentInput input = new SaveStandaloneInstallmentInput(
                "Notebook",
                null,                           // details
                new BigDecimal("6000.00"),
                null,
                "BRL",
                6,
                LocalDate.of(2025, 1, 10),
                "cc-1",
                YearMonth.of(2025, 2),
                FlagEnum.NONE,
                "owner-1"
        );

        ArgumentCaptor<Installment> captor = ArgumentCaptor.forClass(Installment.class);
        when(installmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InstallmentOutput output = useCase.execute(input);

        verify(installmentRepository).save(captor.capture());
        Installment saved = captor.getValue();
        assertThat(saved.getOriginalValue().amount()).isEqualByComparingTo("6000.00");
        assertThat(saved.getInstallmentValue().amount()).isEqualByComparingTo("1000.00");
        assertThat(saved.getSourceWalletId()).isNull();
        assertThat(saved.getSourceExpenseId()).isNull();
        assertThat(saved.getOwnerId()).isEqualTo("owner-1");
        assertThat(output).isNotNull();
    }

    @Test
    void execute_withInstallmentValue_calculatesOriginalValue() {
        SaveStandaloneInstallmentInput input = new SaveStandaloneInstallmentInput(
                "Notebook",
                null,                           // details
                null,
                new BigDecimal("1000.00"),
                "BRL",
                6,
                LocalDate.of(2025, 1, 10),
                "cc-1",
                YearMonth.of(2025, 2),
                FlagEnum.NONE,
                "owner-1"
        );

        ArgumentCaptor<Installment> captor = ArgumentCaptor.forClass(Installment.class);
        when(installmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(input);

        verify(installmentRepository).save(captor.capture());
        Installment saved = captor.getValue();
        assertThat(saved.getInstallmentValue().amount()).isEqualByComparingTo("1000.00");
        assertThat(saved.getOriginalValue().amount()).isEqualByComparingTo("6000.00");
    }

    @Test
    void execute_bothValuesNull_throwsInvalidInput() {
        SaveStandaloneInstallmentInput input = new SaveStandaloneInstallmentInput(
                "Notebook", null, null, null, "BRL", 6,
                LocalDate.of(2025, 1, 10), "cc-1", YearMonth.of(2025, 2), FlagEnum.NONE, "owner-1"
        );

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(InvalidStandaloneInstallmentInputException.class)
                .hasMessageContaining("both are null");
    }

    @Test
    void execute_bothValuesProvided_throwsInvalidInput() {
        SaveStandaloneInstallmentInput input = new SaveStandaloneInstallmentInput(
                "Notebook", null, new BigDecimal("6000.00"), new BigDecimal("1000.00"), "BRL", 6,
                LocalDate.of(2025, 1, 10), "cc-1", YearMonth.of(2025, 2), FlagEnum.NONE, "owner-1"
        );

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(InvalidStandaloneInstallmentInputException.class)
                .hasMessageContaining("both are non-null");
    }

    @Test
    void execute_invalidCurrencyCode_throwsInvalidInput() {
        SaveStandaloneInstallmentInput input = new SaveStandaloneInstallmentInput(
                "Notebook", null, new BigDecimal("6000.00"), null, "INVALID", 6,
                LocalDate.of(2025, 1, 10), "cc-1", YearMonth.of(2025, 2), FlagEnum.NONE, "owner-1"
        );

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(InvalidStandaloneInstallmentInputException.class)
                .hasMessageContaining("Invalid currency code");
    }

    @Test
    void execute_creditCardNotFound_propagates() {
        SaveStandaloneInstallmentInput input = new SaveStandaloneInstallmentInput(
                "Notebook", null, new BigDecimal("6000.00"), null, "BRL", 6,
                LocalDate.of(2025, 1, 10), "missing-cc", YearMonth.of(2025, 2), FlagEnum.NONE, "owner-1"
        );

        doThrow(new CreditCardNotFoundException("missing-cc"))
                .when(findCreditCardByIdBoundary).findById("missing-cc", "owner-1");

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(CreditCardNotFoundException.class);
    }

    @Test
    void execute_creditCardWrongOwner_propagates() {
        SaveStandaloneInstallmentInput input = new SaveStandaloneInstallmentInput(
                "Notebook", null, new BigDecimal("6000.00"), null, "BRL", 6,
                LocalDate.of(2025, 1, 10), "cc-other-owner", YearMonth.of(2025, 2), FlagEnum.NONE, "owner-1"
        );

        doThrow(new CreditCardNotFoundException("cc-other-owner"))
                .when(findCreditCardByIdBoundary).findById("cc-other-owner", "owner-1");

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(CreditCardNotFoundException.class);
    }

    @Test
    void execute_nullFlag_normalizedToNone() {
        SaveStandaloneInstallmentInput input = new SaveStandaloneInstallmentInput(
                "Notebook", null, new BigDecimal("6000.00"), null, "BRL", 6,
                LocalDate.of(2025, 1, 10), "cc-1", YearMonth.of(2025, 2), null, "owner-1"
        );

        ArgumentCaptor<Installment> captor = ArgumentCaptor.forClass(Installment.class);
        when(installmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(input);

        verify(installmentRepository).save(captor.capture());
        assertThat(captor.getValue().getFlag()).isEqualTo(FlagEnum.NONE);
    }

    @Test
    void execute_withDetails_persistsDetails() {
        SaveStandaloneInstallmentInput input = new SaveStandaloneInstallmentInput(
                "Notebook",
                "Comprado na Black Friday",
                new BigDecimal("6000.00"),
                null,
                "BRL",
                6,
                LocalDate.of(2025, 1, 10),
                "cc-1",
                YearMonth.of(2025, 2),
                FlagEnum.NONE,
                "owner-1"
        );

        ArgumentCaptor<Installment> captor = ArgumentCaptor.forClass(Installment.class);
        when(installmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(input);

        verify(installmentRepository).save(captor.capture());
        assertThat(captor.getValue().getDetails()).isEqualTo("Comprado na Black Friday");
    }
}
