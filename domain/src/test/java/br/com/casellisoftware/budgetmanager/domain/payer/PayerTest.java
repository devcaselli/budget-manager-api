package br.com.casellisoftware.budgetmanager.domain.payer;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PayerTest {

    @Test
    void create_standingWithValidInput_initializesPayer() {
        Payer payer = Payer.create(
                "Joao Silva",
                PayerType.STANDING,
                null,
                "sub-1",
                LocalDate.of(2026, 5, 10),
                "owner-1");

        assertThat(payer.getId()).isNotBlank();
        assertThat(payer.getOwnerId()).isEqualTo("owner-1");
        assertThat(payer.getName()).isEqualTo("Joao Silva");
        assertThat(payer.getType()).isEqualTo(PayerType.STANDING);
        assertThat(payer.getWalletId()).isNull();
        assertThat(payer.getSubscriptionId()).isEqualTo("sub-1");
        assertThat(payer.getPaymentDate()).isEqualTo(LocalDate.of(2026, 5, 10));
        assertThat(payer.isDeleted()).isFalse();
    }

    @Test
    void create_transientRequiresWalletId() {
        Payer payer = Payer.create(
                "Maria",
                PayerType.TRANSIENT,
                "wallet-1",
                null,
                LocalDate.of(2026, 5, 10),
                "owner-1");

        assertThat(payer.getType()).isEqualTo(PayerType.TRANSIENT);
        assertThat(payer.getWalletId()).isEqualTo("wallet-1");

        assertThatThrownBy(() -> Payer.create(
                "Maria",
                PayerType.TRANSIENT,
                null,
                null,
                LocalDate.of(2026, 5, 10),
                "owner-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("walletId");
    }

    @Test
    void create_withNullOwner_usesLegacyOwner() {
        Payer payer = Payer.create("Company", PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10), null);

        assertThat(payer.getOwnerId()).isEqualTo(Payer.LEGACY_OWNER_ID);
    }

    @Test
    void create_rejectsInvalidFields() {
        assertThatThrownBy(() -> Payer.create(" ", PayerType.STANDING, null, null, LocalDate.now(), "owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
        assertThatThrownBy(() -> Payer.create("a".repeat(121), PayerType.STANDING, null, null, LocalDate.now(), "owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("120");
        assertThatThrownBy(() -> Payer.create("Name", null, null, null, LocalDate.now(), "owner"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("type");
        assertThatThrownBy(() -> Payer.create("Name", PayerType.STANDING, null, " ", LocalDate.now(), "owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("subscriptionId");
        assertThatThrownBy(() -> Payer.create("Name", PayerType.STANDING, null, null, null, "owner"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("paymentDate");
        assertThatThrownBy(() -> Payer.create("Name", PayerType.STANDING, "wallet-1", null, LocalDate.now(), "owner"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("STANDING");
    }

    @Test
    void mutators_returnNewInstancesAndKeepIdentity() {
        Payer payer = Payer.create("Old", PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10), "owner");

        Payer changed = payer.rename("New")
                .changeLifecycle(PayerType.TRANSIENT, "wallet-1")
                .changeSubscription("sub-1")
                .changePaymentDate(LocalDate.of(2026, 6, 15));

        assertThat(changed.getId()).isEqualTo(payer.getId());
        assertThat(changed.getOwnerId()).isEqualTo(payer.getOwnerId());
        assertThat(changed.getName()).isEqualTo("New");
        assertThat(changed.getType()).isEqualTo(PayerType.TRANSIENT);
        assertThat(changed.getWalletId()).isEqualTo("wallet-1");
        assertThat(changed.getSubscriptionId()).isEqualTo("sub-1");
        assertThat(changed.getPaymentDate()).isEqualTo(LocalDate.of(2026, 6, 15));
    }

    @Test
    void patch_appliesOnlyPresentFields() {
        Payer payer = Payer.create("Old", PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10), "owner");

        Payer patched = payer.patch(new PayerPatch(
                Optional.of("New"),
                Optional.of(PayerType.TRANSIENT),
                Optional.of("wallet-1"),
                Optional.of("sub-1"),
                Optional.empty()));

        assertThat(patched.getName()).isEqualTo("New");
        assertThat(patched.getType()).isEqualTo(PayerType.TRANSIENT);
        assertThat(patched.getWalletId()).isEqualTo("wallet-1");
        assertThat(patched.getSubscriptionId()).isEqualTo("sub-1");
        assertThat(patched.getPaymentDate()).isEqualTo(LocalDate.of(2026, 5, 10));
    }

    @Test
    void delete_isIdempotent() {
        Payer payer = Payer.create("Name", PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10), "owner");

        Payer deleted = payer.delete();

        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.delete()).isSameAs(deleted);
    }

    @Test
    void equality_usesIdAndOwnerId() {
        Payer payer = Payer.create("Name", PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10), "owner-1");

        Payer sameIdentity = new Payer(
                payer.getId(),
                "owner-1",
                "Other Name",
                PayerType.STANDING,
                null,
                null,
                LocalDate.of(2026, 6, 1),
                true);
        Payer otherOwner = new Payer(
                payer.getId(),
                "owner-2",
                "Name",
                PayerType.STANDING,
                null,
                null,
                LocalDate.of(2026, 5, 10),
                false);

        assertThat(payer).isEqualTo(sameIdentity);
        assertThat(payer).hasSameHashCodeAs(sameIdentity);
        assertThat(payer).isNotEqualTo(otherOwner);
    }
}
