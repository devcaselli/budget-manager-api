package br.com.casellisoftware.budgetmanager.persistence.payment;

import br.com.casellisoftware.budgetmanager.AbstractMongoIntegrationTest;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.persistence.payment.mappers.PaymentPersistenceMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Import(PaymentRepositoryImpl.class)
@ComponentScan(basePackageClasses = PaymentPersistenceMapper.class)
class PaymentRepositoryImplTest extends AbstractMongoIntegrationTest {

    private static final Instant PAYMENT_DATE = Instant.parse("2026-04-10T12:00:00Z");

    @Autowired
    private PaymentRepositoryImpl repository;

    @Test
    void save_persistsAllFields() {
        Payment saved = repository.save(newPayment("10.50", "wallet-1", "expense-1", "bullet-1"));

        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getAmount()).isEqualTo(Money.of("10.50"));
        assertThat(saved.getPaymentDate()).isEqualTo(PAYMENT_DATE);
        assertThat(saved.getDetails()).isEqualTo("payment details");
        assertThat(saved.getWalletId()).isEqualTo("wallet-1");
        assertThat(saved.getExpenseId()).isEqualTo("expense-1");
        assertThat(saved.getBulletId()).isEqualTo("bullet-1");
    }

    @Test
    void findById_whenFound_returnsMappedPayment() {
        Payment saved = repository.save(newPayment("5.00", "wallet-1", "expense-1", null));

        Optional<Payment> result = repository.findById(saved.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getAmount()).isEqualTo(Money.of("5.00"));
        assertThat(result.get().getBulletId()).isNull();
    }

    @Test
    void findById_whenMissing_returnsEmpty() {
        assertThat(repository.findById("missing")).isEmpty();
    }

    @Test
    void findByWalletId_returnsPagedResults() {
        repository.save(newPayment("10.00", "wallet-1", "expense-1", "bullet-1"));
        repository.save(newPayment("20.00", "wallet-1", "expense-2", "bullet-2"));
        repository.save(newPayment("30.00", "wallet-2", "expense-3", "bullet-3"));

        PageResult<Payment> result = repository.findByWalletId("wallet-1", 0, 10);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content()).allMatch(payment -> payment.getWalletId().equals("wallet-1"));
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void findByWalletId_respectsPageSize() {
        repository.save(newPayment("10.00", "wallet-1", "expense-1", "bullet-1"));
        repository.save(newPayment("20.00", "wallet-1", "expense-2", "bullet-2"));
        repository.save(newPayment("30.00", "wallet-1", "expense-3", "bullet-3"));

        PageResult<Payment> firstPage = repository.findByWalletId("wallet-1", 0, 2);
        PageResult<Payment> secondPage = repository.findByWalletId("wallet-1", 1, 2);

        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(secondPage.content()).hasSize(1);
        assertThat(secondPage.page()).isEqualTo(1);
    }

    @Test
    void findAllByExpenseId_returnsOnlyPaymentsForExpense() {
        Payment expenseOneFirst = repository.save(newPayment("10.00", "wallet-1", "expense-1", "bullet-1"));
        Payment expenseOneSecond = repository.save(newPayment("20.00", "wallet-1", "expense-1", "bullet-2"));
        Payment expenseTwo = repository.save(newPayment("30.00", "wallet-1", "expense-2", "bullet-3"));

        List<Payment> result = repository.findAllByExpenseId("expense-1");

        assertThat(result)
                .extracting(Payment::getId)
                .containsExactlyInAnyOrder(expenseOneFirst.getId(), expenseOneSecond.getId())
                .doesNotContain(expenseTwo.getId());
    }

    @Test
    void existsByBulletId_returnsWhetherPaymentExistsForBullet() {
        repository.save(newPayment("10.00", "wallet-1", "expense-1", "bullet-1"));

        assertThat(repository.existsByBulletId("bullet-1")).isTrue();
        assertThat(repository.existsByBulletId("missing")).isFalse();
    }

    @Test
    void deleteById_removesPaymentOnlyForOwner() {
        Payment saved = repository.save(newPayment("10.00", "wallet-1", "expense-1", "bullet-1", "owner-1"));

        assertThatThrownBy(() -> repository.deleteById(saved.getId(), "owner-2"))
                .isInstanceOf(PaymentNotFoundException.class);
        assertThat(repository.findById(saved.getId())).isPresent();

        repository.deleteById(saved.getId(), "owner-1");

        assertThat(repository.findById(saved.getId())).isEmpty();
    }

    @Test
    void deleteAllById_removesPayments() {
        Payment first = repository.save(newPayment("10.00", "wallet-1", "expense-1", "bullet-1"));
        Payment second = repository.save(newPayment("20.00", "wallet-1", "expense-2", "bullet-2"));

        repository.deleteAllById(List.of(first.getId(), second.getId()));

        assertThat(repository.findById(first.getId())).isEmpty();
        assertThat(repository.findById(second.getId())).isEmpty();
    }

    private static Payment newPayment(String amount, String walletId, String expenseId, String bulletId) {
        return newPayment(amount, walletId, expenseId, bulletId, Payment.LEGACY_OWNER_ID);
    }

    private static Payment newPayment(String amount, String walletId, String expenseId, String bulletId, String ownerId) {
        return Payment.create(
                Money.of(amount),
                PAYMENT_DATE,
                "payment details",
                expenseId,
                walletId,
                bulletId,
                FlagEnum.NONE,
                ownerId
        );
    }
}
