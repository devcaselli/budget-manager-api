package br.com.casellisoftware.budgetmanager.domain.subscription;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.EndMonthBeforeStartMonthException;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionAlreadyEndedException;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionTest {

    private static final Currency BRL = Currency.getInstance("BRL");
    private static final Currency USD = Currency.getInstance("USD");
    private static final String CC_ID = "cc-1";

    @Test
    void create_initializesSubscriptionForCurrentMonth() {
        Subscription subscription = Subscription.create(
                "Netflix",
                BRL,
                Money.of("55.90"),
                YearMonth.of(2026, 5),
                SubscriptionState.PRODUCTION,
                FlagEnum.NONE,
                CC_ID
        );

        assertThat(subscription.getId()).isNotBlank();
        assertThat(subscription.getDescription()).isEqualTo("Netflix");
        assertThat(subscription.getCurrency()).isEqualTo(BRL);
        assertThat(subscription.getStartMonth()).isEqualTo(YearMonth.of(2026, 5));
        assertThat(subscription.getEndMonth()).isNull();
        assertThat(subscription.getCreditCardId()).isEqualTo(CC_ID);
        assertThat(subscription.getVersions())
                .containsExactly(new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("55.90")));
    }

    @Test
    void create_rejectsNullOrBlankCreditCardId() {
        assertThatThrownBy(() -> Subscription.create("Netflix", BRL, Money.of("50.00"),
                YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("creditCardId");
        assertThatThrownBy(() -> Subscription.create("Netflix", BRL, Money.of("50.00"),
                YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("creditCardId must not be blank");
    }

    @Test
    void rebuild_acceptsNullCreditCardIdForBackCompat() {
        Subscription subscription = Subscription.rebuild(
                "subscription-1",
                "streaming",
                BRL,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("50.00"))),
                FlagEnum.NONE,
                Subscription.LEGACY_OWNER_ID,
                null
        );

        assertThat(subscription.getCreditCardId()).isNull();
    }

    @Test
    void resolveAmount_returnsLatestVersionEffectiveAtTargetMonth() {
        Subscription subscription = Subscription.rebuild(
                "subscription-1",
                "streaming",
                BRL,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(
                        new SubscriptionVersion(YearMonth.of(2026, 7), Money.of("70.00")),
                        new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("50.00")),
                        new SubscriptionVersion(YearMonth.of(2026, 10), Money.of("90.00"))
                ),
                FlagEnum.NONE
        );

        assertThat(subscription.resolveAmount(YearMonth.of(2026, 5))).isEqualTo(Money.of("50.00"));
        assertThat(subscription.resolveAmount(YearMonth.of(2026, 6))).isEqualTo(Money.of("50.00"));
        assertThat(subscription.resolveAmount(YearMonth.of(2026, 7))).isEqualTo(Money.of("70.00"));
        assertThat(subscription.resolveAmount(YearMonth.of(2026, 9))).isEqualTo(Money.of("70.00"));
        assertThat(subscription.resolveAmount(YearMonth.of(2026, 10))).isEqualTo(Money.of("90.00"));
    }

    @Test
    void resolveAmount_whenNoVersionExistsForMonth_throws() {
        Subscription subscription = subscription();

        assertThatThrownBy(() -> subscription.resolveAmount(YearMonth.of(2026, 4)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no subscription version");
    }

    @Test
    void isApplicable_honorsStartMonthAndExclusiveEndMonth() {
        Subscription subscription = subscription().endAt(YearMonth.of(2026, 8));

        assertThat(subscription.isApplicable(YearMonth.of(2026, 4))).isFalse();
        assertThat(subscription.isApplicable(YearMonth.of(2026, 5))).isTrue();
        assertThat(subscription.isApplicable(YearMonth.of(2026, 7))).isTrue();
        assertThat(subscription.isApplicable(YearMonth.of(2026, 8))).isFalse();
        assertThat(subscription.isApplicable(YearMonth.of(2026, 9))).isFalse();
    }

    @Test
    void isApplicable_allowsEndingAtStartMonthBecauseEndMonthIsExclusive() {
        Subscription subscription = subscription().endAt(YearMonth.of(2026, 5));

        assertThat(subscription.isApplicable(YearMonth.of(2026, 5))).isFalse();
    }

    @Test
    void addVersion_appendsFutureVersionWithoutChangingPreviousAmounts() {
        Subscription subscription = subscription();

        Subscription changed = subscription.addVersion(YearMonth.of(2026, 7), Money.of("65.00"));

        assertThat(changed.resolveAmount(YearMonth.of(2026, 6))).isEqualTo(Money.of("50.00"));
        assertThat(changed.resolveAmount(YearMonth.of(2026, 7))).isEqualTo(Money.of("65.00"));
        assertThat(changed.getVersions())
                .containsExactly(
                        new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("50.00")),
                        new SubscriptionVersion(YearMonth.of(2026, 7), Money.of("65.00"))
                );
        assertThat(subscription.getVersions()).hasSize(1);
    }

    @Test
    void addVersion_whenSameMonthAndSameAmount_returnsSameInstance() {
        Subscription subscription = subscription();

        Subscription changed = subscription.addVersion(YearMonth.of(2026, 5), Money.of("50.00"));

        assertThat(changed).isSameAs(subscription);
    }

    @Test
    void addVersion_whenSameMonthAndDifferentAmount_replacesExistingVersion() {
        Subscription subscription = subscription()
                .addVersion(YearMonth.of(2026, 7), Money.of("65.00"));

        Subscription changed = subscription.addVersion(YearMonth.of(2026, 7), Money.of("75.00"));

        assertThat(changed.getVersions())
                .containsExactly(
                        new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("50.00")),
                        new SubscriptionVersion(YearMonth.of(2026, 7), Money.of("75.00"))
                );
        assertThat(changed.resolveAmount(YearMonth.of(2026, 7))).isEqualTo(Money.of("75.00"));
    }

    @Test
    void addVersion_rejectsMonthBeforeStartMonth() {
        Subscription subscription = subscription();

        assertThatThrownBy(() -> subscription.addVersion(YearMonth.of(2026, 4), Money.of("45.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveMonth must not be before startMonth");
    }

    @Test
    void addVersion_rejectsVersionAtOrAfterEndMonth() {
        Subscription subscription = subscription().endAt(YearMonth.of(2026, 8));

        assertThatThrownBy(() -> subscription.addVersion(YearMonth.of(2026, 8), Money.of("45.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("effectiveMonth must be before endMonth");
    }

    @Test
    void addVersion_rejectsCurrencyMismatch() {
        Subscription subscription = subscription();

        assertThatThrownBy(() -> subscription.addVersion(YearMonth.of(2026, 6), usd("45.00")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version currency must match subscription currency");
    }

    @Test
    void applyPatch_renamesAndAddsVersionWhenAmountChanges() {
        Subscription subscription = subscription();

        Subscription patched = subscription.applyPatch(
                SubscriptionPatch.empty()
                        .withDescription("music")
                        .withNewAmount(Money.of("65.00")),
                YearMonth.of(2026, 7)
        );

        assertThat(patched.getDescription()).isEqualTo("music");
        assertThat(patched.resolveAmount(YearMonth.of(2026, 6))).isEqualTo(Money.of("50.00"));
        assertThat(patched.resolveAmount(YearMonth.of(2026, 7))).isEqualTo(Money.of("65.00"));
        assertThat(subscription.getDescription()).isEqualTo("streaming");
    }

    @Test
    void applyPatch_whenValuesDoNotChange_returnsSameInstance() {
        Subscription subscription = subscription();

        Subscription patched = subscription.applyPatch(
                SubscriptionPatch.empty()
                        .withDescription("streaming")
                        .withNewAmount(Money.of("50.00")),
                YearMonth.of(2026, 7)
        );

        assertThat(patched).isSameAs(subscription);
    }

    @Test
    void applyPatch_whenCreditCardChanges_updatesCreditCardId() {
        Subscription subscription = subscription();

        Subscription patched = subscription.applyPatch(
                SubscriptionPatch.empty().withCreditCardId("cc-2"),
                YearMonth.of(2026, 7)
        );

        assertThat(patched.getCreditCardId()).isEqualTo("cc-2");
        assertThat(subscription.getCreditCardId()).isEqualTo(CC_ID);
    }

    @Test
    void applyPatch_rejectsInvalidRequiredFields() {
        Subscription subscription = subscription();

        assertThatThrownBy(() -> subscription.applyPatch(null, YearMonth.of(2026, 7)))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("patch");
        assertThatThrownBy(() -> subscription.applyPatch(SubscriptionPatch.empty(), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currentMonth");
    }

    @Test
    void endAt_setsExclusiveEndMonth() {
        Subscription subscription = subscription();

        Subscription ended = subscription.endAt(YearMonth.of(2026, 8));

        assertThat(ended.getEndMonth()).isEqualTo(YearMonth.of(2026, 8));
        assertThat(subscription.getEndMonth()).isNull();
    }

    @Test
    void endAt_whenAlreadyEnded_throwsSemanticException() {
        Subscription subscription = subscription().endAt(YearMonth.of(2026, 8));

        assertThatThrownBy(() -> subscription.endAt(YearMonth.of(2026, 9)))
                .isInstanceOf(SubscriptionAlreadyEndedException.class)
                .hasMessage("Subscription already ended: " + subscription.getId());
    }

    @Test
    void rename_whenDescriptionIsDifferent_returnsRenamedSubscription() {
        Subscription subscription = subscription();

        Subscription renamed = subscription.rename("music");

        assertThat(renamed.getDescription()).isEqualTo("music");
        assertThat(renamed.getId()).isEqualTo(subscription.getId());
        assertThat(subscription.getDescription()).isEqualTo("streaming");
    }

    @Test
    void rename_whenSameDescription_returnsSameInstance() {
        Subscription subscription = subscription();

        assertThat(subscription.rename("streaming")).isSameAs(subscription);
    }

    @Test
    void rename_rejectsInvalidDescription() {
        Subscription subscription = subscription();

        assertThatThrownBy(() -> subscription.rename(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description must not be null");
        assertThatThrownBy(() -> subscription.rename(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description must not be blank");
    }

    @Test
    void create_rejectsInvalidRequiredFields() {
        assertThatThrownBy(() -> Subscription.create(null, BRL, Money.of("50.00"), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, CC_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description must not be null");
        assertThatThrownBy(() -> Subscription.create(" ", BRL, Money.of("50.00"), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, CC_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description must not be blank");
        assertThatThrownBy(() -> Subscription.create("streaming", null, Money.of("50.00"), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, CC_ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("currency");
        assertThatThrownBy(() -> Subscription.create("streaming", BRL, null, YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, CC_ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
        assertThatThrownBy(() -> Subscription.create("streaming", BRL, Money.zero(), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, CC_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be positive");
        assertThatThrownBy(() -> Subscription.create("streaming", BRL, Money.of("50.00"), null, SubscriptionState.PRODUCTION, FlagEnum.NONE, CC_ID))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("effectiveMonth");
        assertThatThrownBy(() -> Subscription.create("streaming", BRL, usd("50.00"), YearMonth.of(2026, 5), SubscriptionState.PRODUCTION, FlagEnum.NONE, CC_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version currency must match subscription currency");
    }

    @Test
    void addVersion_rejectsInvalidRequiredFields() {
        Subscription subscription = subscription();

        assertThatThrownBy(() -> subscription.addVersion(null, Money.of("55.00")))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("effectiveMonth");
        assertThatThrownBy(() -> subscription.addVersion(YearMonth.of(2026, 6), null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("amount");
        assertThatThrownBy(() -> subscription.addVersion(YearMonth.of(2026, 6), Money.zero()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("amount must be positive");
    }

    @Test
    void endAt_rejectsNullOrMonthBeforeStart() {
        Subscription subscription = subscription();

        assertThatThrownBy(() -> subscription.endAt(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("endMonth");
        assertThatThrownBy(() -> subscription.endAt(YearMonth.of(2026, 4)))
                .isInstanceOf(EndMonthBeforeStartMonthException.class)
                .hasMessageContaining("endMonth must not be before startMonth");
    }

    @Test
    void constructor_rejectsInvalidVersionTimeline() {
        assertThatThrownBy(() -> Subscription.rebuild(
                "subscription-1",
                "streaming",
                BRL,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(YearMonth.of(2026, 6), Money.of("50.00"))),
                FlagEnum.NONE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("versions must include startMonth");
        assertThatThrownBy(() -> Subscription.rebuild(
                "subscription-1",
                "streaming",
                BRL,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(YearMonth.of(2026, 4), Money.of("50.00"))),
                FlagEnum.NONE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("versions must not start before startMonth");
    }

    @Test
    void constructor_rejectsInvalidVersionCollections() {
        assertThatThrownBy(() -> Subscription.rebuild(
                "subscription-1",
                "streaming",
                BRL,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                null,
                FlagEnum.NONE
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("versions");
        assertThatThrownBy(() -> Subscription.rebuild(
                "subscription-1",
                "streaming",
                BRL,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(),
                FlagEnum.NONE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("versions must not be empty");
        assertThatThrownBy(() -> Subscription.rebuild(
                "subscription-1",
                "streaming",
                BRL,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                Arrays.asList(new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("50.00")), null),
                FlagEnum.NONE
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("version");
        assertThatThrownBy(() -> Subscription.rebuild(
                "subscription-1",
                "streaming",
                BRL,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(
                        new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("50.00")),
                        new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("55.00"))
                ),
                FlagEnum.NONE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate effectiveMonth");
    }

    @Test
    void constructor_rejectsEndMonthBeforeStartMonth() {
        assertThatThrownBy(() -> Subscription.rebuild(
                "subscription-1",
                "streaming",
                BRL,
                YearMonth.of(2026, 5),
                YearMonth.of(2026, 4),
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("50.00"))),
                FlagEnum.NONE
        ))
                .isInstanceOf(EndMonthBeforeStartMonthException.class)
                .hasMessageContaining("endMonth must not be before startMonth");
    }

    @Test
    void constructor_rejectsVersionCurrencyMismatch() {
        assertThatThrownBy(() -> Subscription.rebuild(
                "subscription-1",
                "streaming",
                BRL,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(YearMonth.of(2026, 5), usd("50.00"))),
                FlagEnum.NONE
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("version currency must match subscription currency");
    }

    @Test
    void getVersions_returnsImmutableSnapshot() {
        Subscription subscription = subscription();

        assertThatThrownBy(() -> subscription.getVersions()
                .add(new SubscriptionVersion(YearMonth.of(2026, 6), Money.of("55.00"))))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void equalsAndHashCode_areBasedOnId() {
        Subscription first = Subscription.rebuild(
                "subscription-1",
                "streaming",
                BRL,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("50.00"))),
                FlagEnum.NONE
        );
        Subscription second = Subscription.rebuild(
                "subscription-1",
                "music",
                BRL,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("70.00"))),
                FlagEnum.NONE
        );
        Subscription other = Subscription.rebuild(
                "subscription-2",
                "streaming",
                BRL,
                YearMonth.of(2026, 5),
                null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(YearMonth.of(2026, 5), Money.of("50.00"))),
                FlagEnum.NONE
        );

        assertThat(first).isEqualTo(second);
        assertThat(first).isNotEqualTo(other);
        assertThat(first).isNotEqualTo("subscription-1");
        assertThat(first.hashCode()).isEqualTo(second.hashCode());
    }

    private static Subscription subscription() {
        return Subscription.create(
                "streaming",
                BRL,
                Money.of("50.00"),
                YearMonth.of(2026, 5),
                SubscriptionState.PRODUCTION,
                FlagEnum.NONE,
                CC_ID
        );
    }

    private static Money usd(String amount) {
        return Money.of(new BigDecimal(amount), USD);
    }
}
