package br.com.casellisoftware.budgetmanager.application.expense.usecase;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.expense.Expense;
import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindExpensesByWalletIdUseCaseTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private FindWalletByIdBoundary findWalletByIdBoundary;

    private FindExpensesByWalletIdUseCase useCase;

    private static final String WALLET_ID = "wallet-1";
    private static final LocalDate PURCHASE_DATE = LocalDate.now().minusDays(1);

    @BeforeEach
    void setUp() {
        useCase = new FindExpensesByWalletIdUseCase(expenseRepository, findWalletByIdBoundary);
    }

    @Test
    void execute_happyPath_returnsMappedPagedOutput() {
        WalletOutput walletOutput = new WalletOutput(WALLET_ID, "Test Wallet",
                new BigDecimal("100.00"), new BigDecimal("100.00"), null, null, false);
        when(findWalletByIdBoundary.findById(WALLET_ID)).thenReturn(walletOutput);

        Expense expense1 = Expense.create(WALLET_ID, "lunch", Money.of("10.50"), PURCHASE_DATE);
        Expense expense2 = Expense.create(WALLET_ID, "coffee", Money.of("5.00"), PURCHASE_DATE);
        PageResult<Expense> expensePage = new PageResult<>(
                List.of(expense1, expense2), 0, 10, 2, 1
        );
        when(expenseRepository.findByWalletId(WALLET_ID, 0, 10)).thenReturn(expensePage);

        PageResult<ExpenseOutput> result = useCase.execute(WALLET_ID, 0, 10);

        assertThat(result.content()).hasSize(2);
        assertThat(result.content().get(0).name()).isEqualTo("lunch");
        assertThat(result.content().get(0).cost()).isEqualByComparingTo("10.50");
        assertThat(result.content().get(1).name()).isEqualTo("coffee");
        assertThat(result.content().get(1).cost()).isEqualByComparingTo("5.00");
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(10);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);
    }

    @Test
    void execute_emptyPage_returnsEmptyContent() {
        WalletOutput walletOutput = new WalletOutput(WALLET_ID, "Test Wallet",
                new BigDecimal("100.00"), new BigDecimal("100.00"), null, null, false);
        when(findWalletByIdBoundary.findById(WALLET_ID)).thenReturn(walletOutput);

        PageResult<Expense> emptyPage = new PageResult<>(List.of(), 0, 10, 0, 0);
        when(expenseRepository.findByWalletId(WALLET_ID, 0, 10)).thenReturn(emptyPage);

        PageResult<ExpenseOutput> result = useCase.execute(WALLET_ID, 0, 10);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    @Test
    void execute_walletNotFound_propagatesException() {
        when(findWalletByIdBoundary.findById("nonexistent"))
                .thenThrow(new WalletNotFoundException("nonexistent"));

        assertThatThrownBy(() -> useCase.execute("nonexistent", 0, 10))
                .isInstanceOf(WalletNotFoundException.class);

        verifyNoInteractions(expenseRepository);
    }

    @Test
    void execute_repositoryFails_propagates() {
        WalletOutput walletOutput = new WalletOutput(WALLET_ID, "Test Wallet",
                new BigDecimal("100.00"), new BigDecimal("100.00"), null, null, false);
        when(findWalletByIdBoundary.findById(WALLET_ID)).thenReturn(walletOutput);

        RuntimeException boom = new RuntimeException("mongo down");
        when(expenseRepository.findByWalletId(WALLET_ID, 0, 10)).thenThrow(boom);

        assertThatThrownBy(() -> useCase.execute(WALLET_ID, 0, 10)).isSameAs(boom);
    }
}
