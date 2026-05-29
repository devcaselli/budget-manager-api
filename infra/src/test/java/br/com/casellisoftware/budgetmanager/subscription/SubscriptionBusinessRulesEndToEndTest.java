package br.com.casellisoftware.budgetmanager.subscription;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardInput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.SaveCreditCardBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.DeleteSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SaveSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.FindSubscriptionChargesByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.SubscriptionChargeOutput;
import br.com.casellisoftware.budgetmanager.application.shared.AuthenticatedUser;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.SaveWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletAllocationExceededException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletCurrencyMismatchException;
import br.com.casellisoftware.budgetmanager.persistence.subscriptioncharge.SubscriptionChargeDocument;
import br.com.casellisoftware.budgetmanager.persistence.wallet.WalletDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Query;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class SubscriptionBusinessRulesEndToEndTest {

    private static final BigDecimal TWO_HUNDRED = new BigDecimal("200.00");
    private static final BigDecimal ONE_HUNDRED_FIFTY = new BigDecimal("150.00");
    private static final BigDecimal ONE_THOUSAND = new BigDecimal("1000.00");

    @Container
    @ServiceConnection
    static final MongoDBContainer mongo = new MongoDBContainer("mongo:7.0");

    @Autowired
    private SaveSubscriptionBoundary saveSubscriptionBoundary;

    @Autowired
    private SaveCreditCardBoundary saveCreditCardBoundary;

    @Autowired
    private PatchSubscriptionBoundary patchSubscriptionBoundary;

    @Autowired
    private DeleteSubscriptionBoundary deleteSubscriptionBoundary;

    @Autowired
    private SaveWalletBoundary saveWalletBoundary;

    @Autowired
    private FindWalletByIdBoundary findWalletByIdBoundary;

    @Autowired
    private FindSubscriptionChargesByWalletIdBoundary findSubscriptionChargesByWalletIdBoundary;

    @Autowired
    private MongoOperations mongoOperations;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        mongoOperations.getCollectionNames().forEach(name -> mongoOperations.remove(new Query(), name));
        clock.setCurrentMonth(YearMonth.of(2026, 5));
    }

    @Test
    void scenario1_subscriptionStartMonthOnlyAffectsWalletsFromThatMonthOnward() {
        saveSubscription("Netflix", TWO_HUNDRED, "BRL");

        WalletOutput aprilWallet = saveWallet("April", ONE_THOUSAND, LocalDate.of(2026, 4, 1));
        WalletOutput mayWallet = saveWallet("May", ONE_THOUSAND, LocalDate.of(2026, 5, 1));

        assertThat(chargesFor(aprilWallet)).isEmpty();
        assertThat(chargesFor(mayWallet))
                .extracting(SubscriptionChargeOutput::amount)
                .containsExactly(TWO_HUNDRED);
        assertThat(mayWallet.remaining()).isEqualByComparingTo("800.00");
    }

    @Test
    void scenario2_valueVersioningKeepsHistoricalAmountByWalletMonth() {
        SubscriptionOutput subscription = saveSubscription("Streaming", TWO_HUNDRED, "BRL");
        clock.setCurrentMonth(YearMonth.of(2026, 7));
        patchSubscriptionBoundary.execute(new PatchSubscriptionInput(
                subscription.id(),
                null,
                ONE_HUNDRED_FIFTY
        ));

        WalletOutput mayWallet = saveWallet("May", ONE_THOUSAND, LocalDate.of(2026, 5, 1));
        WalletOutput juneWallet = saveWallet("June", ONE_THOUSAND, LocalDate.of(2026, 6, 1));
        WalletOutput julyWallet = saveWallet("July", ONE_THOUSAND, LocalDate.of(2026, 7, 1));

        assertThat(singleChargeAmount(mayWallet)).isEqualByComparingTo("200.00");
        assertThat(singleChargeAmount(juneWallet)).isEqualByComparingTo("200.00");
        assertThat(singleChargeAmount(julyWallet)).isEqualByComparingTo("150.00");
    }

    @Test
    void scenario3_softDeleteEndsSubscriptionExclusivelyAtEndMonth() {
        SubscriptionOutput subscription = saveSubscription("Gym", TWO_HUNDRED, "BRL");
        WalletOutput mayWallet = saveWallet("May", ONE_THOUSAND, LocalDate.of(2026, 5, 1));
        WalletOutput julyWallet = saveWallet("July", ONE_THOUSAND, LocalDate.of(2026, 7, 1));

        clock.setCurrentMonth(YearMonth.of(2026, 8));
        deleteSubscriptionBoundary.execute(subscription.id(), AuthenticatedUser.LEGACY_OWNER_ID);

        WalletOutput augustWallet = saveWallet("August", ONE_THOUSAND, LocalDate.of(2026, 8, 1));

        assertThat(singleChargeAmount(mayWallet)).isEqualByComparingTo("200.00");
        assertThat(singleChargeAmount(julyWallet)).isEqualByComparingTo("200.00");
        assertThat(chargesFor(augustWallet)).isEmpty();
    }

    @Test
    void scenario4_subscriptionOverflowRollsBackWalletAndAlreadySavedCharges() {
        saveSubscription("Subscription A", new BigDecimal("60.00"), "BRL");
        saveSubscription("Subscription B", new BigDecimal("50.00"), "BRL");

        assertThatThrownBy(() -> saveWallet("May", new BigDecimal("100.00"), LocalDate.of(2026, 5, 1)))
                .isInstanceOf(WalletAllocationExceededException.class);

        assertThat(countWallets()).isZero();
        assertThat(countSubscriptionCharges()).isZero();
    }

    @Test
    void scenario5_subscriptionCurrencyMismatchRollsBackWalletCreation() {
        saveSubscription("Dollar Service", new BigDecimal("10.00"), "USD");

        assertThatThrownBy(() -> saveWallet("May", ONE_THOUSAND, LocalDate.of(2026, 5, 1)))
                .isInstanceOf(WalletCurrencyMismatchException.class);

        assertThat(countWallets()).isZero();
        assertThat(countSubscriptionCharges()).isZero();
    }

    @Test
    void scenarioUserReport_subscriptionCreatedAfterWalletDebitsTheExistingWalletRemaining() {
        // Step 1: create wallet first (no subs exist yet -> remaining == budget)
        WalletOutput wallet = saveWallet("May", ONE_THOUSAND, LocalDate.of(2026, 5, 1));
        assertThat(wallet.remaining()).isEqualByComparingTo("1000.00");

        // Step 2: create subscription of 100 in the same effectiveMonth
        saveSubscription("New Service", new BigDecimal("100.00"), "BRL");

        // Step 3: re-fetch wallet -> remaining must have dropped by 100
        WalletOutput refreshed = findWalletByIdBoundary.findById(wallet.id(), AuthenticatedUser.LEGACY_OWNER_ID);
        assertThat(refreshed.remaining()).isEqualByComparingTo("900.00");
        assertThat(chargesFor(refreshed))
                .extracting(SubscriptionChargeOutput::amount)
                .containsExactly(new BigDecimal("100.00"));
    }

    private SubscriptionOutput saveSubscription(String description, BigDecimal amount, String currency) {
        String creditCardId = saveCreditCardBoundary.execute(new CreditCardInput("cc-" + description)).id();
        return saveSubscriptionBoundary.execute(new SubscriptionInput(
                description, amount, currency, null, null,
                br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum.NONE,
                AuthenticatedUser.LEGACY_OWNER_ID,
                creditCardId));
    }

    private WalletOutput saveWallet(String description, BigDecimal budget, LocalDate startDate) {
        return saveWalletBoundary.execute(new WalletInput(description, budget, startDate, null, false, null, null));
    }

    private List<SubscriptionChargeOutput> chargesFor(WalletOutput wallet) {
        return findSubscriptionChargesByWalletIdBoundary.execute(wallet.id(), AuthenticatedUser.LEGACY_OWNER_ID);
    }

    private BigDecimal singleChargeAmount(WalletOutput wallet) {
        return chargesFor(wallet).getFirst().amount();
    }

    private long countWallets() {
        return mongoOperations.count(new Query(), WalletDocument.class);
    }

    private long countSubscriptionCharges() {
        return mongoOperations.count(new Query(), SubscriptionChargeDocument.class);
    }

    @TestConfiguration
    static class ClockTestConfiguration {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock();
        }
    }

    static final class MutableClock extends Clock {

        private static final ZoneId ZONE = ZoneOffset.UTC;

        private Instant instant = YearMonth.of(2026, 5)
                .atDay(1)
                .atStartOfDay(ZONE)
                .toInstant();

        void setCurrentMonth(YearMonth month) {
            this.instant = month.atDay(1).atStartOfDay(ZONE).toInstant();
        }

        @Override
        public ZoneId getZone() {
            return ZONE;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
