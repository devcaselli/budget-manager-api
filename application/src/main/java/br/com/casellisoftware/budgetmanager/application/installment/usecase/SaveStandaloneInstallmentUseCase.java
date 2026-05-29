package br.com.casellisoftware.budgetmanager.application.installment.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutputAssembler;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.SaveStandaloneInstallmentBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.SaveStandaloneInstallmentInput;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InvalidStandaloneInstallmentInputException;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Currency;
import java.util.Objects;

public class SaveStandaloneInstallmentUseCase implements SaveStandaloneInstallmentBoundary {

    private final InstallmentRepository installmentRepository;
    private final FindCreditCardByIdBoundary findCreditCardByIdBoundary;
    private final Clock clock;

    public SaveStandaloneInstallmentUseCase(InstallmentRepository installmentRepository,
                                            FindCreditCardByIdBoundary findCreditCardByIdBoundary,
                                            Clock clock) {
        this.installmentRepository = Objects.requireNonNull(installmentRepository, "installmentRepository must not be null");
        this.findCreditCardByIdBoundary = Objects.requireNonNull(findCreditCardByIdBoundary, "findCreditCardByIdBoundary must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public InstallmentOutput execute(SaveStandaloneInstallmentInput input) {
        Objects.requireNonNull(input, "input must not be null");

        boolean hasOriginalValue = input.originalValue() != null;
        boolean hasInstallmentValue = input.installmentValue() != null;

        if (!hasOriginalValue && !hasInstallmentValue) {
            throw new InvalidStandaloneInstallmentInputException(
                    "Exactly one of originalValue or installmentValue must be provided, but both are null");
        }
        if (hasOriginalValue && hasInstallmentValue) {
            throw new InvalidStandaloneInstallmentInputException(
                    "Exactly one of originalValue or installmentValue must be provided, but both are non-null");
        }

        Currency currency = resolveCurrency(input.currency());

        findCreditCardByIdBoundary.findById(input.creditCardId(), input.ownerId());

        BigDecimal resolvedOriginalValue;
        BigDecimal resolvedInstallmentValue;

        if (hasOriginalValue) {
            resolvedOriginalValue = input.originalValue();
            resolvedInstallmentValue = input.originalValue()
                    .divide(BigDecimal.valueOf(input.installmentNumber()), Money.SCALE, Money.ROUNDING);
        } else {
            resolvedInstallmentValue = input.installmentValue();
            resolvedOriginalValue = input.installmentValue()
                    .multiply(BigDecimal.valueOf(input.installmentNumber()));
        }

        Installment installment = Installment.create(
                input.description(),
                input.details(),
                Money.of(resolvedOriginalValue, currency),
                Money.of(resolvedInstallmentValue, currency),
                input.installmentNumber(),
                input.purchaseDate(),
                input.creditCardId(),
                null,
                null,
                input.sourceEffectiveMonth(),
                input.flag(),
                clock,
                input.ownerId()
        );

        Installment savedInstallment = installmentRepository.save(installment);
        return InstallmentOutputAssembler.from(savedInstallment);
    }

    private static Currency resolveCurrency(String code) {
        try {
            return Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new InvalidStandaloneInstallmentInputException("Invalid currency code: " + code);
        }
    }
}
