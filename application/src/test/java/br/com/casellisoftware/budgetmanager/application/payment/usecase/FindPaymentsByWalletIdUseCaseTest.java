package br.com.casellisoftware.budgetmanager.application.payment.usecase;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.domain.payment.Payment;
import br.com.casellisoftware.budgetmanager.domain.payment.PaymentRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindPaymentsByWalletIdUseCaseTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FindWalletByIdBoundary findWalletByIdBoundary;

    private FindPaymentsByWalletIdUseCase useCase;

    private static final String WALLET_ID = "wallet-1";

    @BeforeEach
    void setUp() {
        useCase = new FindPaymentsByWalletIdUseCase(paymentRepository, findWalletByIdBoundary);
    }

    @Test
    void execute_happyPath_returnsMappedPagedOutput() {
        WalletOutput walletOutput = new WalletOutput(WALLET_ID, "Test Wallet",
                new BigDecimal("1000.00"), new BigDecimal("1000.00"), null, null, false, null, null);
        when(findWalletByIdBoundary.findById(WALLET_ID, "owner-1")).thenReturn(walletOutput);

        Payment p1 = Payment.create(Money.of("10.50"), Instant.now(), "coffee", "expense-1", WALLET_ID, null);
        Payment p2 = Payment.create(Money.of("25.00"), Instant.now(), "lunch", "expense-2", WALLET_ID, "bullet-1");
        PageResult<Payment> page = new PageResult<>(List.of(p1, p2), 0, 10, 2, 1);
        when(paymentRepository.findByWalletId(WALLET_ID, 0, 10, "owner-1")).thenReturn(page);

        PageResult<PaymentOutput> result = useCase.execute(WALLET_ID, 0, 10, "owner-1");

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).walletId()).isEqualTo(WALLET_ID);
        assertThat(result.content().get(0).amount().amount()).isEqualByComparingTo("10.50");
        assertThat(result.content().get(1).amount().amount()).isEqualByComparingTo("25.00");
        assertThat(result.content().get(1).bulletId()).isEqualTo("bullet-1");
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void execute_emptyPage_returnsEmptyContent() {
        WalletOutput walletOutput = new WalletOutput(WALLET_ID, "Test Wallet",
                new BigDecimal("1000.00"), new BigDecimal("1000.00"), null, null, false, null, null);
        when(findWalletByIdBoundary.findById(WALLET_ID, "owner-1")).thenReturn(walletOutput);

        PageResult<Payment> emptyPage = new PageResult<>(List.of(), 0, 10, 0, 0);
        when(paymentRepository.findByWalletId(WALLET_ID, 0, 10, "owner-1")).thenReturn(emptyPage);

        PageResult<PaymentOutput> result = useCase.execute(WALLET_ID, 0, 10, "owner-1");

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void execute_walletNotFound_propagatesExceptionWithNoRepositoryCall() {
        when(findWalletByIdBoundary.findById("nonexistent", "owner-1"))
                .thenThrow(new WalletNotFoundException("nonexistent"));

        assertThatThrownBy(() -> useCase.execute("nonexistent", 0, 10, "owner-1"))
                .isInstanceOf(WalletNotFoundException.class);

        verifyNoInteractions(paymentRepository);
    }

    @Test
    void execute_repositoryFails_propagates() {
        WalletOutput walletOutput = new WalletOutput(WALLET_ID, "Test Wallet",
                new BigDecimal("1000.00"), new BigDecimal("1000.00"), null, null, false, null, null);
        when(findWalletByIdBoundary.findById(WALLET_ID, "owner-1")).thenReturn(walletOutput);

        RuntimeException boom = new RuntimeException("mongo down");
        when(paymentRepository.findByWalletId(WALLET_ID, 0, 10, "owner-1")).thenThrow(boom);

        assertThatThrownBy(() -> useCase.execute(WALLET_ID, 0, 10, "owner-1")).isSameAs(boom);
    }
}
