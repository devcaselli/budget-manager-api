package br.com.casellisoftware.budgetmanager.domain.subscription;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagAware;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.EndMonthBeforeStartMonthException;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionAlreadyEndedException;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Domain entity representing a recurring subscription.
 *
 * <p>Immutable: every state-changing operation returns a new instance.
 * New subscriptions should be obtained via {@link #create}; reconstruction from
 * persistence uses {@link #rebuild}.</p>
 */
public final class Subscription implements FlagAware {

    public static final String LEGACY_OWNER_ID = "legacy";

    private final String id;
    private final String ownerId;
    private final String description;
    private final Currency currency;
    private final YearMonth startMonth;
    private final YearMonth endMonth;
    private final SubscriptionState state;
    private final List<SubscriptionVersion> versions;
    private final FlagEnum flag;
    private final String creditCardId;

    public Subscription(String id,
                        String description,
                        Currency currency,
                        YearMonth startMonth,
                        YearMonth endMonth,
                        SubscriptionState state,
                        List<SubscriptionVersion> versions,
                        FlagEnum flag) {
        this(id, LEGACY_OWNER_ID, description, currency, startMonth, endMonth, state, versions, flag, null);
    }

    public Subscription(String id,
                        String ownerId,
                        String description,
                        Currency currency,
                        YearMonth startMonth,
                        YearMonth endMonth,
                        SubscriptionState state,
                        List<SubscriptionVersion> versions,
                        FlagEnum flag) {
        this(id, ownerId, description, currency, startMonth, endMonth, state, versions, flag, null);
    }

    public Subscription(String id,
                        String ownerId,
                        String description,
                        Currency currency,
                        YearMonth startMonth,
                        YearMonth endMonth,
                        SubscriptionState state,
                        List<SubscriptionVersion> versions,
                        FlagEnum flag,
                        String creditCardId) {
        this.id = id;
        this.ownerId = requireNonBlank(ownerId, "ownerId");
        this.description = validateDescription(description);
        this.currency = Objects.requireNonNull(currency, "currency must not be null");
        this.startMonth = Objects.requireNonNull(startMonth, "startMonth must not be null");
        this.endMonth = endMonth;
        this.state = Objects.requireNonNull(state, "state must not be null");
        if (endMonth != null && endMonth.isBefore(startMonth)) {
            throw new EndMonthBeforeStartMonthException(startMonth, endMonth);
        }
        this.versions = normalizeVersions(versions);
        validateVersionTimeline(this.startMonth, this.versions);
        validateVersionCurrencies(this.currency, this.versions);
        this.flag = flag == null ? FlagEnum.NONE : flag;
        this.creditCardId = normalizeCreditCardId(creditCardId);
    }

    public static Subscription create(String description,
                                      Currency currency,
                                      Money initialAmount,
                                      YearMonth effectiveMonth,
                                      SubscriptionState state,
                                      FlagEnum flag,
                                      String creditCardId) {
        return create(description, currency, initialAmount, effectiveMonth, state, flag, LEGACY_OWNER_ID, creditCardId);
    }

    public static Subscription create(String description,
                                      Currency currency,
                                      Money initialAmount,
                                      YearMonth effectiveMonth,
                                      SubscriptionState state,
                                      FlagEnum flag,
                                      String ownerId,
                                      String creditCardId) {
        Objects.requireNonNull(currency, "currency must not be null");
        YearMonth startMonth = Objects.requireNonNull(effectiveMonth, "effectiveMonth must not be null");
        String requiredCreditCardId = requireNonBlank(creditCardId, "creditCardId");
        SubscriptionVersion initialVersion = new SubscriptionVersion(startMonth, initialAmount);
        return new Subscription(
                UUID.randomUUID().toString(),
                ownerId,
                description,
                currency,
                startMonth,
                null,
                Objects.requireNonNull(state, "state must not be null"),
                List.of(initialVersion),
                flag,
                requiredCreditCardId
        );
    }

    public static Subscription rebuild(String id,
                                       String description,
                                       Currency currency,
                                       YearMonth startMonth,
                                       YearMonth endMonth,
                                       SubscriptionState state,
                                       List<SubscriptionVersion> versions,
                                       FlagEnum flag) {
        return rebuild(id, description, currency, startMonth, endMonth, state, versions, flag, LEGACY_OWNER_ID, null);
    }

    public static Subscription rebuild(String id,
                                       String description,
                                       Currency currency,
                                       YearMonth startMonth,
                                       YearMonth endMonth,
                                       SubscriptionState state,
                                       List<SubscriptionVersion> versions,
                                       FlagEnum flag,
                                       String ownerId) {
        return rebuild(id, description, currency, startMonth, endMonth, state, versions, flag, ownerId, null);
    }

    public static Subscription rebuild(String id,
                                       String description,
                                       Currency currency,
                                       YearMonth startMonth,
                                       YearMonth endMonth,
                                       SubscriptionState state,
                                       List<SubscriptionVersion> versions,
                                       FlagEnum flag,
                                       String ownerId,
                                       String creditCardId) {
        return new Subscription(id, ownerId, description, currency, startMonth, endMonth, state, versions, flag, creditCardId);
    }

    public Subscription rename(String description) {
        String renamedDescription = validateDescription(description);
        if (Objects.equals(this.description, renamedDescription)) {
            return this;
        }
        return new Subscription(this.id, this.ownerId, renamedDescription, this.currency, this.startMonth, this.endMonth, this.state, this.versions, this.flag, this.creditCardId);
    }

    public Subscription withCreditCardId(String creditCardId) {
        String normalized = requireNonBlank(creditCardId, "creditCardId");
        if (Objects.equals(this.creditCardId, normalized)) {
            return this;
        }
        return new Subscription(this.id, this.ownerId, this.description, this.currency, this.startMonth, this.endMonth, this.state, this.versions, this.flag, normalized);
    }

    public Subscription addVersion(YearMonth effectiveMonth, Money amount) {
        YearMonth normalizedMonth = Objects.requireNonNull(effectiveMonth, "effectiveMonth must not be null");
        if (normalizedMonth.isBefore(this.startMonth)) {
            throw new IllegalArgumentException("effectiveMonth must not be before startMonth");
        }
        if (this.endMonth != null && !normalizedMonth.isBefore(this.endMonth)) {
            throw new IllegalArgumentException("effectiveMonth must be before endMonth");
        }

        SubscriptionVersion newVersion = new SubscriptionVersion(normalizedMonth, amount);
        validateVersionCurrency(this.currency, newVersion);
        SubscriptionVersion existingVersion = this.versions.stream()
                .filter(version -> version.effectiveMonth().equals(normalizedMonth))
                .findFirst()
                .orElse(null);

        if (Objects.equals(existingVersion, newVersion)) {
            return this;
        }

        List<SubscriptionVersion> updatedVersions = this.versions.stream()
                .filter(version -> !version.effectiveMonth().equals(normalizedMonth))
                .collect(Collectors.toCollection(ArrayList::new));
        updatedVersions.add(newVersion);

        return new Subscription(this.id, this.ownerId, this.description, this.currency, this.startMonth, this.endMonth, this.state, updatedVersions, this.flag, this.creditCardId);
    }

    public Subscription applyPatch(SubscriptionPatch patch, YearMonth currentMonth) {
        Objects.requireNonNull(patch, "patch must not be null");
        YearMonth effectiveMonth = Objects.requireNonNull(currentMonth, "currentMonth must not be null");
        if (patch.isEmpty()) {
            return this;
        }

        Subscription patched = this;
        if (patch.flag().isPresent() && !Objects.equals(this.flag, patch.flag().get())) {
            patched = new Subscription(this.id, this.ownerId, this.description, this.currency, this.startMonth, this.endMonth, this.state, this.versions, patch.flag().get(), this.creditCardId);
        }
        if (patch.description().isPresent()) {
            patched = patched.rename(patch.description().get());
        }
        if (patch.newAmount().isPresent() && !patch.newAmount().get().equals(patched.resolveAmount(effectiveMonth))) {
            patched = patched.addVersion(effectiveMonth, patch.newAmount().get());
        }
        if (patch.creditCardId().isPresent()) {
            patched = patched.withCreditCardId(patch.creditCardId().get());
        }
        return patched;
    }

    public Subscription endAt(YearMonth endMonth) {
        YearMonth normalizedEndMonth = Objects.requireNonNull(endMonth, "endMonth must not be null");
        if (this.endMonth != null) {
            throw new SubscriptionAlreadyEndedException(this.id);
        }
        if (normalizedEndMonth.isBefore(this.startMonth)) {
            throw new EndMonthBeforeStartMonthException(this.startMonth, normalizedEndMonth);
        }
        return new Subscription(this.id, this.ownerId, this.description, this.currency, this.startMonth, normalizedEndMonth, this.state, this.versions, this.flag, this.creditCardId);
    }

    public Money resolveAmount(YearMonth month) {
        YearMonth targetMonth = Objects.requireNonNull(month, "month must not be null");
        for (int i = this.versions.size() - 1; i >= 0; i--) {
            SubscriptionVersion version = this.versions.get(i);
            if (!version.effectiveMonth().isAfter(targetMonth)) {
                return version.amount();
            }
        }
        throw new IllegalArgumentException("no subscription version for month: " + targetMonth);
    }

    public boolean isApplicable(YearMonth month) {
        YearMonth targetMonth = Objects.requireNonNull(month, "month must not be null");
        return !targetMonth.isBefore(this.startMonth)
                && (this.endMonth == null || targetMonth.isBefore(this.endMonth));
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

    public Currency getCurrency() {
        return currency;
    }

    public YearMonth getStartMonth() {
        return startMonth;
    }

    public YearMonth getEndMonth() {
        return endMonth;
    }

    public SubscriptionState getState() {
        return state;
    }

    public List<SubscriptionVersion> getVersions() {
        return versions;
    }

    @Override
    public FlagEnum getFlag() {
        return flag;
    }

    public String getCreditCardId() {
        return creditCardId;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Subscription subscription
                && Objects.equals(id, subscription.id)
                && Objects.equals(ownerId, subscription.ownerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, ownerId);
    }

    private static String normalizeCreditCardId(String creditCardId) {
        if (creditCardId == null) {
            return null;
        }
        if (creditCardId.isBlank()) {
            throw new IllegalArgumentException("creditCardId must not be blank");
        }
        return creditCardId;
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

    private static List<SubscriptionVersion> normalizeVersions(List<SubscriptionVersion> versions) {
        Objects.requireNonNull(versions, "versions must not be null");
        if (versions.isEmpty()) {
            throw new IllegalArgumentException("versions must not be empty");
        }
        Set<YearMonth> seen = new HashSet<>();
        for (SubscriptionVersion version : versions) {
            Objects.requireNonNull(version, "version must not be null");
            if (!seen.add(version.effectiveMonth())) {
                throw new IllegalArgumentException("versions must not contain duplicate effectiveMonth values");
            }
        }
        return versions.stream()
                .sorted(Comparator.comparing(SubscriptionVersion::effectiveMonth))
                .collect(Collectors.toUnmodifiableList());
    }

    private static void validateVersionTimeline(YearMonth startMonth, List<SubscriptionVersion> versions) {
        YearMonth firstEffectiveMonth = versions.getFirst().effectiveMonth();
        if (firstEffectiveMonth.isBefore(startMonth)) {
            throw new IllegalArgumentException("versions must not start before startMonth");
        }
        if (firstEffectiveMonth.isAfter(startMonth)) {
            throw new IllegalArgumentException("versions must include startMonth");
        }
    }

    private static void validateVersionCurrencies(Currency currency, List<SubscriptionVersion> versions) {
        versions.forEach(version -> validateVersionCurrency(currency, version));
    }

    private static void validateVersionCurrency(Currency currency, SubscriptionVersion version) {
        if (!currency.equals(version.amount().currency())) {
            throw new IllegalArgumentException(
                    "version currency must match subscription currency: "
                            + version.amount().currency() + " vs " + currency);
        }
    }
}
