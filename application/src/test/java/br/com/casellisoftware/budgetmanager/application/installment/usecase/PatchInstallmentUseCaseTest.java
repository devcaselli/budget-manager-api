package br.com.casellisoftware.budgetmanager.application.installment.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.PatchInstallmentInput;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.LastInstallmentDateCalculator;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Currency;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchInstallmentUseCaseTest {

    private static final String OWNER_ID = "owner-1";
    private static final String INSTALLMENT_ID = "inst-1";
    private static final String CREDIT_CARD_ID = "cc-1";
    private static final YearMonth SOURCE_MONTH = YearMonth.of(2026, 1);
    private static final int INSTALLMENT_NUMBER = 6;
    private static final YearMonth LAST_DATE = LastInstallmentDateCalculator.calculate(SOURCE_MONTH, INSTALLMENT_NUMBER);
    private static final Money ORIGINAL_VALUE = Money.of(new BigDecimal("6000.00"), Currency.getInstance("BRL"));
    private static final Money INSTALLMENT_VALUE = Money.of(new BigDecimal("1000.00"), Currency.getInstance("BRL"));

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private FindCreditCardByIdBoundary findCreditCardByIdBoundary;

    private PatchInstallmentUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new PatchInstallmentUseCase(installmentRepository, findCreditCardByIdBoundary);
    }

    @Test
    void execute_patchDetails_updatesDetailsOnly() {
        Installment existing = existingInstallment();
        when(installmentRepository.findById(INSTALLMENT_ID, OWNER_ID)).thenReturn(Optional.of(existing));
        when(installmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PatchInstallmentInput input = new PatchInstallmentInput(
                INSTALLMENT_ID, "new details", null, null, null, null, null, null, null, OWNER_ID);

        InstallmentOutput output = useCase.execute(input);

        assertThat(output.details()).isEqualTo("new details");
        assertThat(output.installmentNumber()).isEqualTo(INSTALLMENT_NUMBER);
        assertThat(output.originalValue()).isEqualByComparingTo("6000.00");
        assertThat(output.installmentValue()).isEqualByComparingTo("1000.00");
        verify(findCreditCardByIdBoundary, never()).findById(any(), any());
    }

    @Test
    void execute_patchInstallmentNumber_recalculatesInstallmentValueAndLastDate() {
        Installment existing = existingInstallment();
        when(installmentRepository.findById(INSTALLMENT_ID, OWNER_ID)).thenReturn(Optional.of(existing));
        ArgumentCaptor<Installment> captor = ArgumentCaptor.forClass(Installment.class);
        when(installmentRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        PatchInstallmentInput input = new PatchInstallmentInput(
                INSTALLMENT_ID, null, null, null, null, 12, null, null, null, OWNER_ID);

        InstallmentOutput output = useCase.execute(input);

        // originalValue unchanged (6000), installmentValue = 6000/12 = 500
        assertThat(output.installmentNumber()).isEqualTo(12);
        assertThat(output.installmentValue()).isEqualByComparingTo("500.00");
        assertThat(output.originalValue()).isEqualByComparingTo("6000.00");
        // lastInstallmentDate = SOURCE_MONTH + (12-1) months
        assertThat(output.lastInstallmentDate()).isEqualTo(LastInstallmentDateCalculator.calculate(SOURCE_MONTH, 12));
    }

    @Test
    void execute_patchOriginalValue_recalculatesInstallmentValue() {
        Installment existing = existingInstallment();
        when(installmentRepository.findById(INSTALLMENT_ID, OWNER_ID)).thenReturn(Optional.of(existing));
        when(installmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PatchInstallmentInput input = new PatchInstallmentInput(
                INSTALLMENT_ID, null, null, new BigDecimal("12000.00"), null, null, null, null, null, OWNER_ID);

        InstallmentOutput output = useCase.execute(input);

        // installmentValue = 12000 / 6 = 2000
        assertThat(output.originalValue()).isEqualByComparingTo("12000.00");
        assertThat(output.installmentValue()).isEqualByComparingTo("2000.00");
        assertThat(output.installmentNumber()).isEqualTo(INSTALLMENT_NUMBER);
    }

    @Test
    void execute_patchInstallmentValue_recalculatesOriginalValue() {
        Installment existing = existingInstallment();
        when(installmentRepository.findById(INSTALLMENT_ID, OWNER_ID)).thenReturn(Optional.of(existing));
        when(installmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PatchInstallmentInput input = new PatchInstallmentInput(
                INSTALLMENT_ID, null, null, null, new BigDecimal("1500.00"), null, null, null, null, OWNER_ID);

        InstallmentOutput output = useCase.execute(input);

        // originalValue = 1500 * 6 = 9000
        assertThat(output.installmentValue()).isEqualByComparingTo("1500.00");
        assertThat(output.originalValue()).isEqualByComparingTo("9000.00");
    }

    @Test
    void execute_patchSourceEffectiveMonth_recalculatesLastDate() {
        Installment existing = existingInstallment();
        when(installmentRepository.findById(INSTALLMENT_ID, OWNER_ID)).thenReturn(Optional.of(existing));
        when(installmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        YearMonth newStartMonth = YearMonth.of(2026, 6);
        PatchInstallmentInput input = new PatchInstallmentInput(
                INSTALLMENT_ID, null, null, null, null, null, newStartMonth, null, null, OWNER_ID);

        InstallmentOutput output = useCase.execute(input);

        assertThat(output.sourceEffectiveMonth()).isEqualTo(newStartMonth);
        assertThat(output.lastInstallmentDate())
                .isEqualTo(LastInstallmentDateCalculator.calculate(newStartMonth, INSTALLMENT_NUMBER));
    }

    @Test
    void execute_patchCreditCardId_validatesOwnership() {
        Installment existing = existingInstallment();
        when(installmentRepository.findById(INSTALLMENT_ID, OWNER_ID)).thenReturn(Optional.of(existing));
        when(installmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PatchInstallmentInput input = new PatchInstallmentInput(
                INSTALLMENT_ID, null, null, null, null, null, null, null, "cc-new", OWNER_ID);

        InstallmentOutput output = useCase.execute(input);

        assertThat(output.creditCardId()).isEqualTo("cc-new");
        verify(findCreditCardByIdBoundary).findById("cc-new", OWNER_ID);
    }

    @Test
    void execute_patchCreditCardId_whenCardNotFound_throws() {
        Installment existing = existingInstallment();
        when(installmentRepository.findById(INSTALLMENT_ID, OWNER_ID)).thenReturn(Optional.of(existing));
        doThrow(new CreditCardNotFoundException("cc-bad"))
                .when(findCreditCardByIdBoundary).findById("cc-bad", OWNER_ID);

        PatchInstallmentInput input = new PatchInstallmentInput(
                INSTALLMENT_ID, null, null, null, null, null, null, null, "cc-bad", OWNER_ID);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(CreditCardNotFoundException.class);
        verify(installmentRepository, never()).save(any());
    }

    @Test
    void execute_whenInstallmentNotFound_throws() {
        when(installmentRepository.findById(INSTALLMENT_ID, OWNER_ID)).thenReturn(Optional.empty());

        PatchInstallmentInput input = new PatchInstallmentInput(
                INSTALLMENT_ID, null, null, null, null, null, null, null, null, OWNER_ID);

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(InstallmentNotFoundException.class);
        verify(installmentRepository, never()).save(any());
    }

    @Test
    void execute_patchPurchaseDate_updatesDate() {
        Installment existing = existingInstallment();
        when(installmentRepository.findById(INSTALLMENT_ID, OWNER_ID)).thenReturn(Optional.of(existing));
        when(installmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LocalDate newDate = LocalDate.of(2026, 3, 15);
        PatchInstallmentInput input = new PatchInstallmentInput(
                INSTALLMENT_ID, null, null, null, null, null, null, newDate, null, OWNER_ID);

        InstallmentOutput output = useCase.execute(input);

        assertThat(output.purchaseDate()).isEqualTo(newDate);
    }

    private static Installment existingInstallment() {
        return Installment.rebuild(
                INSTALLMENT_ID,
                "Notebook",
                ORIGINAL_VALUE,
                INSTALLMENT_VALUE,
                INSTALLMENT_NUMBER,
                LocalDate.of(2026, 1, 10),
                LAST_DATE,
                CREDIT_CARD_ID,
                "wallet-1",
                SOURCE_MONTH,
                false,
                null,
                FlagEnum.NONE
        );
    }
}
