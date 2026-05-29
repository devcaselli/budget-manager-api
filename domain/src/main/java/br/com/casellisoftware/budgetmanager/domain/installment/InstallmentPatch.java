package br.com.casellisoftware.budgetmanager.domain.installment;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable value object carrying the fields to be patched on an {@link Installment}.
 *
 * <p>All components are {@code Optional.empty()} by default (use {@link #empty()}).
 * Only present fields will overwrite the existing entity values.</p>
 *
 * <p>Constraint: {@code originalValue} and {@code installmentValue} are mutually exclusive —
 * providing both throws {@link InvalidInstallmentPatchException}.</p>
 */
public record InstallmentPatch(
        Optional<String> details,
        Optional<FlagEnum> flag,
        Optional<Money> originalValue,
        Optional<Money> installmentValue,
        Optional<Integer> installmentNumber,
        Optional<YearMonth> sourceEffectiveMonth,
        Optional<LocalDate> purchaseDate,
        Optional<String> creditCardId
) {
    public InstallmentPatch {
        Objects.requireNonNull(details, "details must not be null");
        Objects.requireNonNull(flag, "flag must not be null");
        Objects.requireNonNull(originalValue, "originalValue must not be null");
        Objects.requireNonNull(installmentValue, "installmentValue must not be null");
        Objects.requireNonNull(installmentNumber, "installmentNumber must not be null");
        Objects.requireNonNull(sourceEffectiveMonth, "sourceEffectiveMonth must not be null");
        Objects.requireNonNull(purchaseDate, "purchaseDate must not be null");
        Objects.requireNonNull(creditCardId, "creditCardId must not be null");

        if (originalValue.isPresent() && installmentValue.isPresent()) {
            throw new InvalidInstallmentPatchException(
                    "Exactly one of originalValue or installmentValue may be patched, but both are present");
        }
    }

    public static InstallmentPatch empty() {
        return new InstallmentPatch(
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()
        );
    }

    public InstallmentPatch withDetails(String details) {
        return details == null ? this : new InstallmentPatch(Optional.of(details), flag, originalValue, installmentValue, installmentNumber, sourceEffectiveMonth, purchaseDate, creditCardId);
    }

    public InstallmentPatch withFlag(FlagEnum flag) {
        return flag == null || flag == FlagEnum.NONE
                ? this
                : new InstallmentPatch(details, Optional.of(flag), originalValue, installmentValue, installmentNumber, sourceEffectiveMonth, purchaseDate, creditCardId);
    }

    public InstallmentPatch withOriginalValue(Money originalValue) {
        return originalValue == null ? this : new InstallmentPatch(details, flag, Optional.of(originalValue), installmentValue, installmentNumber, sourceEffectiveMonth, purchaseDate, creditCardId);
    }

    public InstallmentPatch withInstallmentValue(Money installmentValue) {
        return installmentValue == null ? this : new InstallmentPatch(details, flag, originalValue, Optional.of(installmentValue), installmentNumber, sourceEffectiveMonth, purchaseDate, creditCardId);
    }

    public InstallmentPatch withInstallmentNumber(Integer installmentNumber) {
        return installmentNumber == null ? this : new InstallmentPatch(details, flag, originalValue, installmentValue, Optional.of(installmentNumber), sourceEffectiveMonth, purchaseDate, creditCardId);
    }

    public InstallmentPatch withSourceEffectiveMonth(YearMonth sourceEffectiveMonth) {
        return sourceEffectiveMonth == null ? this : new InstallmentPatch(details, flag, originalValue, installmentValue, installmentNumber, Optional.of(sourceEffectiveMonth), purchaseDate, creditCardId);
    }

    public InstallmentPatch withPurchaseDate(LocalDate purchaseDate) {
        return purchaseDate == null ? this : new InstallmentPatch(details, flag, originalValue, installmentValue, installmentNumber, sourceEffectiveMonth, Optional.of(purchaseDate), creditCardId);
    }

    public InstallmentPatch withCreditCardId(String creditCardId) {
        return creditCardId == null ? this : new InstallmentPatch(details, flag, originalValue, installmentValue, installmentNumber, sourceEffectiveMonth, purchaseDate, Optional.of(creditCardId));
    }

    public boolean isEmpty() {
        return details.isEmpty() && flag.isEmpty() && originalValue.isEmpty() && installmentValue.isEmpty()
                && installmentNumber.isEmpty() && sourceEffectiveMonth.isEmpty() && purchaseDate.isEmpty()
                && creditCardId.isEmpty();
    }

    public List<String> appliedFieldNames() {
        List<String> fields = new ArrayList<>();
        details.ifPresent(ignored -> fields.add("details"));
        flag.ifPresent(ignored -> fields.add("flag"));
        originalValue.ifPresent(ignored -> fields.add("originalValue"));
        installmentValue.ifPresent(ignored -> fields.add("installmentValue"));
        installmentNumber.ifPresent(ignored -> fields.add("installmentNumber"));
        sourceEffectiveMonth.ifPresent(ignored -> fields.add("sourceEffectiveMonth"));
        purchaseDate.ifPresent(ignored -> fields.add("purchaseDate"));
        creditCardId.ifPresent(ignored -> fields.add("creditCardId"));
        return List.copyOf(fields);
    }
}
