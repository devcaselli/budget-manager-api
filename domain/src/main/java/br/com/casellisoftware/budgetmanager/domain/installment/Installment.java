package br.com.casellisoftware.budgetmanager.domain.installment;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagAware;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Objects;
import java.util.UUID;

/**
 * Domain entity representing an installment-based expense.
 *
 * <p>Immutable. Use {@link #create} for new instances and {@link #rebuild} for
 * persistence reconstruction.</p>
 */
public record Installment(
        String id,
        String ownerId,
        String description,
        String details,
        Money originalValue,
        Money installmentValue,
        int installmentNumber,
        LocalDate purchaseDate,
        YearMonth lastInstallmentDate,
        String creditCardId,
        String sourceExpenseId,
        String sourceWalletId,
        YearMonth sourceEffectiveMonth,
        boolean deleted,
        LocalDateTime deletedAt,
        FlagEnum flag
) implements FlagAware {

    public static final String LEGACY_OWNER_ID = "legacy";

    public Installment {
        id = Objects.requireNonNull(id, "id must not be null");
        ownerId = requireNonBlank(ownerId, "ownerId");
        description = validateDescription(description);
        originalValue = Objects.requireNonNull(originalValue, "originalValue must not be null");
        installmentValue = Objects.requireNonNull(installmentValue, "installmentValue must not be null");
        purchaseDate = Objects.requireNonNull(purchaseDate, "purchaseDate must not be null");
        lastInstallmentDate = Objects.requireNonNull(lastInstallmentDate, "lastInstallmentDate must not be null");
        creditCardId = requireNonBlank(creditCardId, "creditCardId");
        sourceExpenseId = normalizeSourceExpenseId(sourceExpenseId);
        sourceWalletId = normalizeSourceWalletId(sourceWalletId);
        sourceEffectiveMonth = Objects.requireNonNull(sourceEffectiveMonth, "sourceEffectiveMonth must not be null");
        flag = flag == null ? FlagEnum.NONE : flag;

        validateInvariants(
                originalValue,
                installmentValue,
                installmentNumber,
                lastInstallmentDate,
                sourceEffectiveMonth,
                deleted,
                deletedAt
        );
    }

    public Installment(String id,
                       String description,
                       Money originalValue,
                       Money installmentValue,
                       int installmentNumber,
                       LocalDate purchaseDate,
                       YearMonth lastInstallmentDate,
                       String creditCardId,
                       String sourceExpenseId,
                       String sourceWalletId,
                       YearMonth sourceEffectiveMonth,
                       boolean deleted,
                       LocalDateTime deletedAt,
                       FlagEnum flag) {
        this(
                id,
                LEGACY_OWNER_ID,
                description,
                null,
                originalValue,
                installmentValue,
                installmentNumber,
                purchaseDate,
                lastInstallmentDate,
                creditCardId,
                sourceExpenseId,
                sourceWalletId,
                sourceEffectiveMonth,
                deleted,
                deletedAt,
                flag
        );
    }

    public static Installment create(String description,
                                     Money originalValue,
                                     Money installmentValue,
                                     int installmentNumber,
                                     LocalDate purchaseDate,
                                     String creditCardId,
                                     String sourceWalletId,
                                     YearMonth sourceEffectiveMonth,
                                     FlagEnum flag) {
        return create(
                description,
                null,
                originalValue,
                installmentValue,
                installmentNumber,
                purchaseDate,
                creditCardId,
                null,
                sourceWalletId,
                sourceEffectiveMonth,
                flag,
                Clock.systemDefaultZone(),
                LEGACY_OWNER_ID
        );
    }

    public static Installment create(String description,
                                     Money originalValue,
                                     Money installmentValue,
                                     int installmentNumber,
                                     LocalDate purchaseDate,
                                     String creditCardId,
                                     String sourceExpenseId,
                                     String sourceWalletId,
                                     YearMonth sourceEffectiveMonth,
                                     FlagEnum flag) {
        return create(
                description,
                null,
                originalValue,
                installmentValue,
                installmentNumber,
                purchaseDate,
                creditCardId,
                sourceExpenseId,
                sourceWalletId,
                sourceEffectiveMonth,
                flag,
                Clock.systemDefaultZone(),
                LEGACY_OWNER_ID
        );
    }

    public static Installment create(String description,
                                     Money originalValue,
                                     Money installmentValue,
                                     int installmentNumber,
                                     LocalDate purchaseDate,
                                     String creditCardId,
                                     String sourceExpenseId,
                                     String sourceWalletId,
                                     YearMonth sourceEffectiveMonth,
                                     FlagEnum flag,
                                     Clock clock) {
        return create(description, null, originalValue, installmentValue, installmentNumber, purchaseDate, creditCardId,
                sourceExpenseId, sourceWalletId, sourceEffectiveMonth, flag, clock, LEGACY_OWNER_ID);
    }

    public static Installment create(String description,
                                     Money originalValue,
                                     Money installmentValue,
                                     int installmentNumber,
                                     LocalDate purchaseDate,
                                     String creditCardId,
                                     String sourceExpenseId,
                                     String sourceWalletId,
                                     YearMonth sourceEffectiveMonth,
                                     FlagEnum flag,
                                     Clock clock,
                                     String ownerId) {
        return create(description, null, originalValue, installmentValue, installmentNumber, purchaseDate, creditCardId,
                sourceExpenseId, sourceWalletId, sourceEffectiveMonth, flag, clock, ownerId);
    }

    public static Installment create(String description,
                                     String details,
                                     Money originalValue,
                                     Money installmentValue,
                                     int installmentNumber,
                                     LocalDate purchaseDate,
                                     String creditCardId,
                                     String sourceExpenseId,
                                     String sourceWalletId,
                                     YearMonth sourceEffectiveMonth,
                                     FlagEnum flag,
                                     Clock clock,
                                     String ownerId) {
        Objects.requireNonNull(clock, "clock must not be null");
        if (purchaseDate.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("purchaseDate must not be in the future");
        }
        YearMonth lastInstallmentDate = LastInstallmentDateCalculator.calculate(sourceEffectiveMonth, installmentNumber);
        return new Installment(
                UUID.randomUUID().toString(),
                ownerId,
                description,
                details,
                originalValue,
                installmentValue,
                installmentNumber,
                purchaseDate,
                lastInstallmentDate,
                creditCardId,
                sourceExpenseId,
                sourceWalletId,
                sourceEffectiveMonth,
                false,
                null,
                flag
        );
    }

    public static Installment rebuild(String id,
                                      String description,
                                      Money originalValue,
                                      Money installmentValue,
                                      int installmentNumber,
                                      LocalDate purchaseDate,
                                      YearMonth lastInstallmentDate,
                                      String creditCardId,
                                      String sourceWalletId,
                                      YearMonth sourceEffectiveMonth,
                                      boolean deleted,
                                      LocalDateTime deletedAt,
                                      FlagEnum flag) {
        return rebuild(
                id,
                description,
                null,
                originalValue,
                installmentValue,
                installmentNumber,
                purchaseDate,
                lastInstallmentDate,
                creditCardId,
                null,
                sourceWalletId,
                sourceEffectiveMonth,
                deleted,
                deletedAt,
                flag
        );
    }

    public static Installment rebuild(String id,
                                      String description,
                                      String details,
                                      Money originalValue,
                                      Money installmentValue,
                                      int installmentNumber,
                                      LocalDate purchaseDate,
                                      YearMonth lastInstallmentDate,
                                      String creditCardId,
                                      String sourceExpenseId,
                                      String sourceWalletId,
                                      YearMonth sourceEffectiveMonth,
                                      boolean deleted,
                                      LocalDateTime deletedAt,
                                      FlagEnum flag) {
        return new Installment(
                id,
                LEGACY_OWNER_ID,
                description,
                details,
                originalValue,
                installmentValue,
                installmentNumber,
                purchaseDate,
                lastInstallmentDate,
                creditCardId,
                sourceExpenseId,
                sourceWalletId,
                sourceEffectiveMonth,
                deleted,
                deletedAt,
                flag
        );
    }

    public static Installment rebuild(String id,
                                      String description,
                                      String details,
                                      Money originalValue,
                                      Money installmentValue,
                                      int installmentNumber,
                                      LocalDate purchaseDate,
                                      YearMonth lastInstallmentDate,
                                      String creditCardId,
                                      String sourceExpenseId,
                                      String sourceWalletId,
                                      YearMonth sourceEffectiveMonth,
                                      boolean deleted,
                                      LocalDateTime deletedAt,
                                      FlagEnum flag,
                                      String ownerId) {
        return new Installment(
                id,
                ownerId,
                description,
                details,
                originalValue,
                installmentValue,
                installmentNumber,
                purchaseDate,
                lastInstallmentDate,
                creditCardId,
                sourceExpenseId,
                sourceWalletId,
                sourceEffectiveMonth,
                deleted,
                deletedAt,
                flag
        );
    }

    public Installment delete(Clock clock) {
        Objects.requireNonNull(clock, "clock must not be null");
        if (this.deleted) {
            throw new InstallmentAlreadyDeletedException(this.id);
        }
        return new Installment(
                this.id,
                this.ownerId,
                this.description,
                this.details,
                this.originalValue,
                this.installmentValue,
                this.installmentNumber,
                this.purchaseDate,
                this.lastInstallmentDate,
                this.creditCardId,
                this.sourceExpenseId,
                this.sourceWalletId,
                this.sourceEffectiveMonth,
                true,
                LocalDateTime.now(clock),
                this.flag
        );
    }

    public Installment patch(InstallmentPatch patch) {
        Objects.requireNonNull(patch, "patch must not be null");

        int resolvedNumber = patch.installmentNumber().orElse(this.installmentNumber);
        YearMonth resolvedEffectiveMonth = patch.sourceEffectiveMonth().orElse(this.sourceEffectiveMonth);
        YearMonth resolvedLastInstallmentDate = LastInstallmentDateCalculator.calculate(resolvedEffectiveMonth, resolvedNumber);

        // Value resolution: exactly one of originalValue / installmentValue may be patched.
        // The other is recalculated. If neither is patched, keep existing values unless
        // installmentNumber changed — in that case recalculate installmentValue from originalValue.
        Money resolvedOriginalValue;
        Money resolvedInstallmentValue;

        if (patch.originalValue().isPresent()) {
            resolvedOriginalValue = patch.originalValue().get();
            resolvedInstallmentValue = Money.of(
                    resolvedOriginalValue.amount().divide(
                            BigDecimal.valueOf(resolvedNumber), Money.SCALE, Money.ROUNDING),
                    resolvedOriginalValue.currency()
            );
        } else if (patch.installmentValue().isPresent()) {
            resolvedInstallmentValue = patch.installmentValue().get();
            resolvedOriginalValue = Money.of(
                    resolvedInstallmentValue.amount().multiply(BigDecimal.valueOf(resolvedNumber)),
                    resolvedInstallmentValue.currency()
            );
        } else if (patch.installmentNumber().isPresent()) {
            // Number changed but no value provided — recalculate installmentValue from existing originalValue
            resolvedOriginalValue = this.originalValue;
            resolvedInstallmentValue = Money.of(
                    this.originalValue.amount().divide(
                            BigDecimal.valueOf(resolvedNumber), Money.SCALE, Money.ROUNDING),
                    this.originalValue.currency()
            );
        } else {
            resolvedOriginalValue = this.originalValue;
            resolvedInstallmentValue = this.installmentValue;
        }

        return new Installment(
                this.id,
                this.ownerId,
                this.description,
                patch.details().orElse(this.details),
                resolvedOriginalValue,
                resolvedInstallmentValue,
                resolvedNumber,
                patch.purchaseDate().orElse(this.purchaseDate),
                resolvedLastInstallmentDate,
                patch.creditCardId().orElse(this.creditCardId),
                this.sourceExpenseId,
                this.sourceWalletId,
                resolvedEffectiveMonth,
                this.deleted,
                this.deletedAt,
                patch.flag().orElse(this.flag)
        );
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

    public String getDetails() {
        return details;
    }

    public Money getOriginalValue() {
        return originalValue;
    }

    public Money getInstallmentValue() {
        return installmentValue;
    }

    public int getInstallmentNumber() {
        return installmentNumber;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public YearMonth getLastInstallmentDate() {
        return lastInstallmentDate;
    }

    public String getCreditCardId() {
        return creditCardId;
    }

    public String getSourceExpenseId() {
        return sourceExpenseId;
    }

    public String getSourceWalletId() {
        return sourceWalletId;
    }

    public boolean hasPerMonthExpense() {
        return sourceWalletId != null;
    }

    public YearMonth getSourceEffectiveMonth() {
        return sourceEffectiveMonth;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    @Override
    public FlagEnum getFlag() {
        return flag;
    }

    private static void validateInvariants(Money originalValue,
                                           Money installmentValue,
                                           int installmentNumber,
                                           YearMonth lastInstallmentDate,
                                           YearMonth sourceEffectiveMonth,
                                           boolean deleted,
                                           LocalDateTime deletedAt) {
        InstallmentNumberPolicy.validate(installmentNumber);
        if (!installmentValue.currency().equals(originalValue.currency())) {
            throw new IllegalArgumentException(
                    "installmentValue currency must match originalValue currency: "
                            + installmentValue.currency() + " vs " + originalValue.currency());
        }
        if (!installmentValue.isPositive()) {
            throw new IllegalArgumentException("installmentValue must be positive");
        }

        BigDecimal sum = installmentValue.amount().multiply(BigDecimal.valueOf(installmentNumber));
        BigDecimal diff = sum.subtract(originalValue.amount()).abs();
        BigDecimal tolerance = InstallmentNumberPolicy.roundingToleranceFor(installmentNumber);
        if (diff.compareTo(tolerance) > 0) {
            throw new IllegalArgumentException(
                    "installmentValue * installmentNumber (" + sum + ") deviates from originalValue ("
                            + originalValue.amount() + ") by " + diff + " > tolerance " + tolerance);
        }

        YearMonth expected = LastInstallmentDateCalculator.calculate(sourceEffectiveMonth, installmentNumber);
        if (!expected.equals(lastInstallmentDate)) {
            throw new IllegalArgumentException(
                    "lastInstallmentDate (" + lastInstallmentDate + ") must equal "
                            + "sourceEffectiveMonth.plusMonths(installmentNumber - 1) (" + expected + ")");
        }

        if (deleted && deletedAt == null) {
            throw new IllegalArgumentException("deletedAt must not be null when deleted is true");
        }
        if (!deleted && deletedAt != null) {
            throw new IllegalArgumentException("deletedAt must be null when deleted is false");
        }
    }

    private static String validateDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        return description;
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value;
    }

    private static String normalizeSourceExpenseId(String sourceExpenseId) {
        if (sourceExpenseId == null) {
            return null;
        }
        return requireNonBlank(sourceExpenseId, "sourceExpenseId");
    }

    private static String normalizeSourceWalletId(String sourceWalletId) {
        if (sourceWalletId == null) {
            return null;
        }
        return requireNonBlank(sourceWalletId, "sourceWalletId");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Installment other
                && Objects.equals(id, other.id)
                && Objects.equals(ownerId, other.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ownerId);
    }
}
