package br.com.casellisoftware.budgetmanager.domain.reservedbudget;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagAware;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Domain entity representing a reserved slice of budget ("Reserved Budget").
 *
 * <p>A reserved budget is <b>wallet-agnostic and cross-month</b>: it is born in a
 * {@code startMonth} ({@code effectiveMonth}) and reduces the available budget of every
 * wallet from that month onward, without replicating records per wallet. The deduction is
 * computed at read time (see {@code ReservedBudgetWalletBalanceCalculator}); no wallet is
 * eagerly debited.</p>
 *
 * <p>The reserved amount carries a variation history: a list of {@link ReservedBudgetVersion}
 * (one per {@code effectiveMonth}) such that {@link #resolveAmount(YearMonth)} returns the
 * amount effective for any queried month. Changing the amount appends a new version from a
 * given month onward, leaving prior months untouched.</p>
 *
 * <p>Immutable: every state-changing operation returns a new instance. New entities should be
 * obtained via {@link #create}; reconstruction from persistence uses {@link #rebuild}.
 * Deletion is logical: {@link #markDeleted(LocalDateTime)} (mirrors {@code ExtraBudget}).</p>
 */
public final class ReservedBudget implements FlagAware {

    public static final String LEGACY_OWNER_ID = "legacy";

    private final String id;
    private final String ownerId;
    private final String description;
    private final String details;
    private final Currency currency;
    private final YearMonth startMonth;
    private final List<ReservedBudgetVersion> versions;
    private final List<ReservedBudgetLink> links;
    private final boolean deleted;
    private final LocalDateTime deletedAt;
    private final FlagEnum flag;

    private ReservedBudget(String id,
                           String ownerId,
                           String description,
                           String details,
                           Currency currency,
                           YearMonth startMonth,
                           List<ReservedBudgetVersion> versions,
                           List<ReservedBudgetLink> links,
                           boolean deleted,
                           LocalDateTime deletedAt,
                           FlagEnum flag) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.ownerId = requireNonBlank(ownerId, "ownerId");
        this.description = validateDescription(description);
        this.details = normalizeDetails(details);
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.startMonth = Objects.requireNonNull(startMonth, "startMonth must not be null");
        this.versions = normalizeVersions(versions);
        validateVersionTimeline(this.startMonth, this.versions);
        validateVersionCurrencies(this.currency, this.versions);
        this.links = normalizeLinks(links);
        validateLinksFromMonth(this.startMonth, this.links);
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        this.flag = flag == null ? FlagEnum.NONE : flag;
    }

    public static ReservedBudget create(String description,
                                        String details,
                                        Currency currency,
                                        Money initialAmount,
                                        YearMonth effectiveMonth,
                                        FlagEnum flag,
                                        String ownerId) {
        Objects.requireNonNull(currency, "currency must not be null");
        YearMonth startMonth = Objects.requireNonNull(effectiveMonth, "effectiveMonth must not be null");
        ReservedBudgetVersion initialVersion = new ReservedBudgetVersion(startMonth, initialAmount);
        return new ReservedBudget(
                UUID.randomUUID().toString(),
                ownerId,
                description,
                details,
                currency,
                startMonth,
                List.of(initialVersion),
                List.of(),
                false,
                null,
                flag
        );
    }

    public static ReservedBudget rebuild(String id,
                                         String ownerId,
                                         String description,
                                         String details,
                                         Currency currency,
                                         YearMonth startMonth,
                                         List<ReservedBudgetVersion> versions,
                                         List<ReservedBudgetLink> links,
                                         boolean deleted,
                                         LocalDateTime deletedAt,
                                         FlagEnum flag) {
        return new ReservedBudget(id, ownerId, description, details, currency, startMonth, versions, links, deleted, deletedAt, flag);
    }

    public ReservedBudget rename(String description) {
        String renamed = validateDescription(description);
        if (Objects.equals(this.description, renamed)) {
            return this;
        }
        return new ReservedBudget(this.id, this.ownerId, renamed, this.details, this.currency, this.startMonth, this.versions, this.links, this.deleted, this.deletedAt, this.flag);
    }

    public ReservedBudget withDetails(String details) {
        String normalized = normalizeDetails(details);
        if (Objects.equals(this.details, normalized)) {
            return this;
        }
        return new ReservedBudget(this.id, this.ownerId, this.description, normalized, this.currency, this.startMonth, this.versions, this.links, this.deleted, this.deletedAt, this.flag);
    }

    /**
     * Appends (or replaces) the reserved amount effective from {@code effectiveMonth} onward.
     * Prior months keep their previously effective amount. Re-applying the same amount on the
     * same month is a no-op.
     */
    public ReservedBudget addVersion(YearMonth effectiveMonth, Money amount) {
        YearMonth normalizedMonth = Objects.requireNonNull(effectiveMonth, "effectiveMonth must not be null");
        if (normalizedMonth.isBefore(this.startMonth)) {
            throw new IllegalArgumentException("effectiveMonth must not be before startMonth");
        }

        ReservedBudgetVersion newVersion = new ReservedBudgetVersion(normalizedMonth, amount);
        validateVersionCurrency(this.currency, newVersion);
        ReservedBudgetVersion existingVersion = this.versions.stream()
                .filter(version -> version.effectiveMonth().equals(normalizedMonth))
                .findFirst()
                .orElse(null);

        if (Objects.equals(existingVersion, newVersion)) {
            return this;
        }

        List<ReservedBudgetVersion> updatedVersions = this.versions.stream()
                .filter(version -> !version.effectiveMonth().equals(normalizedMonth))
                .collect(Collectors.toCollection(ArrayList::new));
        updatedVersions.add(newVersion);

        return new ReservedBudget(this.id, this.ownerId, this.description, this.details, this.currency, this.startMonth, updatedVersions, this.links, this.deleted, this.deletedAt, this.flag);
    }

    /**
     * Applies a patch, materializing an amount change as a new version effective from
     * {@code currentMonth} onward (mirrors {@code Subscription.applyPatch}).
     */
    public ReservedBudget applyPatch(ReservedBudgetPatch patch, YearMonth currentMonth) {
        Objects.requireNonNull(patch, "patch must not be null");
        YearMonth effectiveMonth = Objects.requireNonNull(currentMonth, "currentMonth must not be null");
        if (patch.isEmpty()) {
            return this;
        }

        ReservedBudget patched = this;
        if (patch.flag().isPresent() && !Objects.equals(this.flag, patch.flag().get())) {
            patched = new ReservedBudget(this.id, this.ownerId, this.description, this.details, this.currency, this.startMonth, this.versions, this.links, this.deleted, this.deletedAt, patch.flag().get());
        }
        if (patch.description().isPresent()) {
            patched = patched.rename(patch.description().get());
        }
        if (patch.details().isPresent()) {
            patched = patched.withDetails(patch.details().get());
        }
        if (patch.newAmount().isPresent() && !patch.newAmount().get().equals(patched.resolveAmount(effectiveMonth))) {
            patched = patched.addVersion(effectiveMonth, patch.newAmount().get());
        }
        return patched;
    }

    /**
     * Marks this reserved budget as logically deleted, returning a new instance.
     * Idempotent: returns {@code this} if already deleted (mirrors {@code ExtraBudget}).
     */
    public ReservedBudget markDeleted(LocalDateTime now) {
        Objects.requireNonNull(now, "now must not be null");
        if (this.deleted) {
            return this;
        }
        return new ReservedBudget(this.id, this.ownerId, this.description, this.details, this.currency, this.startMonth, this.versions, this.links, true, now, this.flag);
    }

    /**
     * Returns the reserved amount effective for {@code month}: the amount of the latest
     * version whose {@code effectiveMonth} is not after {@code month}.
     */
    public Money resolveAmount(YearMonth month) {
        YearMonth targetMonth = Objects.requireNonNull(month, "month must not be null");
        for (int i = this.versions.size() - 1; i >= 0; i--) {
            ReservedBudgetVersion version = this.versions.get(i);
            if (!version.effectiveMonth().isAfter(targetMonth)) {
                return version.amount();
            }
        }
        throw new IllegalArgumentException("no reserved-budget version for month: " + targetMonth);
    }

    /**
     * A reserved budget applies to every month from {@code startMonth} onward (no end).
     */
    public boolean isApplicable(YearMonth month) {
        YearMonth targetMonth = Objects.requireNonNull(month, "month must not be null");
        return !targetMonth.isBefore(this.startMonth);
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

    public Currency getCurrency() {
        return currency;
    }

    public YearMonth getStartMonth() {
        return startMonth;
    }

    public List<ReservedBudgetVersion> getVersions() {
        return versions;
    }

    public List<ReservedBudgetLink> getLinks() {
        return links;
    }

    /**
     * Returns the link for the given {@code (sourceType, sourceId)} pair, if present.
     */
    public Optional<ReservedBudgetLink> findLink(ReservedBudgetLinkSourceType sourceType, String sourceId) {
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        return links.stream()
                .filter(l -> l.sourceType() == sourceType && l.sourceId().equals(sourceId))
                .findFirst();
    }

    /**
     * Returns a new {@link ReservedBudget} with the given link added or replaced (same key =
     * same {@code sourceType + sourceId}).
     *
     * <p>Invariant: {@code link.fromMonth()} must not be before {@link #startMonth}.</p>
     */
    public ReservedBudget addLink(ReservedBudgetLink link) {
        Objects.requireNonNull(link, "link must not be null");
        if (link.fromMonth().isBefore(this.startMonth)) {
            throw new IllegalArgumentException(
                    "link.fromMonth must not be before startMonth: " + link.fromMonth() + " < " + this.startMonth);
        }
        List<ReservedBudgetLink> updated = this.links.stream()
                .filter(l -> !(l.sourceType() == link.sourceType() && l.sourceId().equals(link.sourceId())))
                .collect(Collectors.toCollection(ArrayList::new));
        updated.add(link);
        return new ReservedBudget(this.id, this.ownerId, this.description, this.details, this.currency,
                this.startMonth, this.versions, updated, this.deleted, this.deletedAt, this.flag);
    }

    /**
     * Returns a new {@link ReservedBudget} with the link for {@code (sourceType, sourceId)} removed.
     * No-op if the link does not exist.
     */
    public ReservedBudget removeLink(ReservedBudgetLinkSourceType sourceType, String sourceId) {
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(sourceId, "sourceId must not be null");
        List<ReservedBudgetLink> updated = this.links.stream()
                .filter(l -> !(l.sourceType() == sourceType && l.sourceId().equals(sourceId)))
                .toList();
        if (updated.size() == this.links.size()) {
            return this;
        }
        return new ReservedBudget(this.id, this.ownerId, this.description, this.details, this.currency,
                this.startMonth, this.versions, updated, this.deleted, this.deletedAt, this.flag);
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

    @Override
    public boolean equals(Object o) {
        return o instanceof ReservedBudget reservedBudget
                && Objects.equals(id, reservedBudget.id)
                && Objects.equals(ownerId, reservedBudget.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ownerId);
    }

    private static String normalizeDetails(String details) {
        if (details == null) {
            return null;
        }
        String trimmed = details.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName + " must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String validateDescription(String description) {
        if (description == null) {
            throw new IllegalArgumentException("description must not be null");
        }
        if (description.isBlank()) {
            throw new IllegalArgumentException("description must not be blank");
        }
        return description;
    }

    private static List<ReservedBudgetVersion> normalizeVersions(List<ReservedBudgetVersion> versions) {
        Objects.requireNonNull(versions, "versions must not be null");
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("versions must not be empty");
        }
        Set<YearMonth> seen = new HashSet<>();
        for (ReservedBudgetVersion version : versions) {
            Objects.requireNonNull(version, "version must not be null");
            if (!seen.add(version.effectiveMonth())) {
                throw new IllegalArgumentException("versions must not contain duplicate effectiveMonth values");
            }
        }
        return versions.stream()
                .sorted(Comparator.comparing(ReservedBudgetVersion::effectiveMonth))
                .collect(Collectors.toUnmodifiableList());
    }

    private static void validateVersionTimeline(YearMonth startMonth, List<ReservedBudgetVersion> versions) {
        YearMonth firstEffectiveMonth = versions.getFirst().effectiveMonth();
        if (firstEffectiveMonth.isBefore(startMonth)) {
            throw new IllegalArgumentException("versions must not start before startMonth");
        }
        if (firstEffectiveMonth.isAfter(startMonth)) {
            throw new IllegalArgumentException("versions must include startMonth");
        }
    }

    private static void validateVersionCurrencies(Currency currency, List<ReservedBudgetVersion> versions) {
        versions.forEach(version -> validateVersionCurrency(currency, version));
    }

    private static void validateVersionCurrency(Currency currency, ReservedBudgetVersion version) {
        if (!currency.equals(version.amount().currency())) {
            throw new IllegalArgumentException(
                    "version currency must match reserved-budget currency: "
                            + version.amount().currency() + " vs " + currency);
        }
    }

    /**
     * Normalizes the links list: null → empty, rejects duplicate {@code (sourceType, sourceId)}
     * pairs, and produces a stable ordering (by sourceType then sourceId).
     */
    private static List<ReservedBudgetLink> normalizeLinks(List<ReservedBudgetLink> links) {
        if (links == null) {
            return List.of();
        }
        Set<String> seen = new HashSet<>();
        for (ReservedBudgetLink link : links) {
            Objects.requireNonNull(link, "link must not be null");
            String key = link.sourceType().name() + ":" + link.sourceId();
            if (!seen.add(key)) {
                throw new IllegalArgumentException(
                        "links must not contain duplicate (sourceType, sourceId): " + key);
            }
        }
        return links.stream()
                .sorted(Comparator.comparing((ReservedBudgetLink l) -> l.sourceType().name())
                        .thenComparing(ReservedBudgetLink::sourceId))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Validates that all link {@code fromMonth} values are not before {@code startMonth}.
     */
    private static void validateLinksFromMonth(YearMonth startMonth, List<ReservedBudgetLink> links) {
        for (ReservedBudgetLink link : links) {
            if (link.fromMonth().isBefore(startMonth)) {
                throw new IllegalArgumentException(
                        "link.fromMonth must not be before startMonth: "
                                + link.fromMonth() + " < " + startMonth);
            }
        }
    }
}
