package br.com.casellisoftware.budgetmanager.domain.subscriptioncharge;

import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.subscription.Subscription;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionState;
import br.com.casellisoftware.budgetmanager.domain.subscription.SubscriptionVersion;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Currency;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionChargePreviewIdTest {

    private static final String WALLET_ID = "wallet-1";
    private static final String SUBSCRIPTION_ID = "sub-1";
    private static final YearMonth MONTH = YearMonth.of(2026, 5);

    @Test
    void constructor_requiresNonNullWalletId() {
        assertThrows(NullPointerException.class, () ->
                new SubscriptionChargePreviewId(null, SUBSCRIPTION_ID, MONTH)
        );
    }

    @Test
    void constructor_requiresNonNullSubscriptionId() {
        assertThrows(NullPointerException.class, () ->
                new SubscriptionChargePreviewId(WALLET_ID, null, MONTH)
        );
    }

    @Test
    void constructor_requiresNonNullMonth() {
        assertThrows(NullPointerException.class, () ->
                new SubscriptionChargePreviewId(WALLET_ID, SUBSCRIPTION_ID, null)
        );
    }

    @Test
    void of_createsPreviewIdFromWalletAndSubscription() {
        Currency usd = Currency.getInstance("USD");
        Money budget = Money.of(new BigDecimal("1000.00"), usd);
        Wallet wallet = Wallet.create(
                "Test Wallet",
                budget,
                null,
                null,
                false,
                MONTH,
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );

        Subscription subscription = Subscription.rebuild(
                SUBSCRIPTION_ID,
                "Test Subscription",
                usd,
                MONTH,
                null,
                SubscriptionState.PRODUCTION,
                List.of(new SubscriptionVersion(MONTH, Money.of(new BigDecimal("100.00"), usd))),
                FlagEnum.NONE
        );

        SubscriptionChargePreviewId id = SubscriptionChargePreviewId.of(wallet, subscription);

        assertNotNull(id);
        assertEquals(wallet.getId(), id.walletId());
        assertEquals(SUBSCRIPTION_ID, id.subscriptionId());
        assertEquals(MONTH, id.month());
    }

    @Test
    void asString_returnsCompositeIdInCorrectFormat() {
        SubscriptionChargePreviewId id = new SubscriptionChargePreviewId(WALLET_ID, SUBSCRIPTION_ID, MONTH);

        String result = id.asString();

        assertEquals("wallet-1:sub-1:2026-05", result);
    }

    @Test
    void toString_delegatesToAsString() {
        SubscriptionChargePreviewId id = new SubscriptionChargePreviewId(WALLET_ID, SUBSCRIPTION_ID, MONTH);

        assertEquals(id.asString(), id.toString());
    }

    @Test
    void equals_comparesValuesByContent() {
        SubscriptionChargePreviewId id1 = new SubscriptionChargePreviewId(WALLET_ID, SUBSCRIPTION_ID, MONTH);
        SubscriptionChargePreviewId id2 = new SubscriptionChargePreviewId(WALLET_ID, SUBSCRIPTION_ID, MONTH);
        SubscriptionChargePreviewId id3 = new SubscriptionChargePreviewId("other-wallet", SUBSCRIPTION_ID, MONTH);

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
    }

    @Test
    void hashCode_isConsistentWithEquals() {
        SubscriptionChargePreviewId id1 = new SubscriptionChargePreviewId(WALLET_ID, SUBSCRIPTION_ID, MONTH);
        SubscriptionChargePreviewId id2 = new SubscriptionChargePreviewId(WALLET_ID, SUBSCRIPTION_ID, MONTH);

        assertEquals(id1.hashCode(), id2.hashCode());
    }
}
