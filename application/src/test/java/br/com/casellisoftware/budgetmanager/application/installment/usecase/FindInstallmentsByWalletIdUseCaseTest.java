package br.com.casellisoftware.budgetmanager.application.installment.usecase;

import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentWalletFilter;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.Installment;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentRepository;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentSortOrder;
import br.com.casellisoftware.budgetmanager.domain.installment.LastInstallmentDateCalculator;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindInstallmentsByWalletIdUseCaseTest {

    private static final String WALLET_ID = "wallet-1";
    private static final String OWNER_ID = "owner-1";
    private static final YearMonth EFFECTIVE_MONTH = YearMonth.of(2026, 6);

    // Source month deliberately before EFFECTIVE_MONTH so installments affect the wallet.
    // lastInstallmentDate is computed via LastInstallmentDateCalculator to stay consistent
    // with domain invariants regardless of semantic subtleties in sourceEffectiveMonth.
    private static final YearMonth SOURCE_MONTH = YearMonth.of(2026, 3);
    private static final int INSTALLMENT_NUMBER = 6;
    private static final YearMonth LAST_INSTALLMENT_DATE =
            LastInstallmentDateCalculator.calculate(SOURCE_MONTH, INSTALLMENT_NUMBER);

    @Mock
    private InstallmentRepository installmentRepository;

    @Mock
    private FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    @Mock
    private ShareRepository shareRepository;

    private FindInstallmentsByWalletIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindInstallmentsByWalletIdUseCase(installmentRepository, findWalletDomainByIdBoundary, shareRepository);
        org.mockito.Mockito.lenient()
                .when(shareRepository.findActiveBySourceId(any(ShareSourceType.class), anyString(), anyString()))
                .thenReturn(Optional.empty());
    }

    @Test
    void execute_returnsPagedResultFromRepository() {
        Wallet wallet = wallet(WALLET_ID, EFFECTIVE_MONTH);
        Installment inst1 = installment("inst-1", WALLET_ID);
        Installment inst2 = installment("inst-2", "wallet-other");
        PageResult<Installment> repoPage = new PageResult<>(List.of(inst1, inst2), 0, 20, 2, 1);

        when(findWalletDomainByIdBoundary.findById(WALLET_ID, OWNER_ID)).thenReturn(wallet);
        when(installmentRepository.findByWalletContext(
                eq(WALLET_ID), eq(EFFECTIVE_MONTH), isNull(),
                eq(InstallmentSortOrder.ENDING_SOON), eq(0), eq(20), eq(OWNER_ID)))
                .thenReturn(repoPage);

        InstallmentWalletFilter filter = new InstallmentWalletFilter(0, 20, null, InstallmentSortOrder.ENDING_SOON);
        PageResult<InstallmentOutput> result = useCase.execute(WALLET_ID, filter, OWNER_ID);

        assertThat(result.content())
                .extracting(InstallmentOutput::id)
                .containsExactly("inst-1", "inst-2");
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.page()).isZero();
        assertThat(result.size()).isEqualTo(20);
    }

    @Test
    void execute_withCreditCardFilter_passesFilterToRepository() {
        Wallet wallet = wallet(WALLET_ID, EFFECTIVE_MONTH);
        PageResult<Installment> repoPage = new PageResult<>(List.of(), 0, 20, 0, 0);

        when(findWalletDomainByIdBoundary.findById(WALLET_ID, OWNER_ID)).thenReturn(wallet);
        when(installmentRepository.findByWalletContext(
                eq(WALLET_ID), eq(EFFECTIVE_MONTH), eq("cc-42"),
                eq(InstallmentSortOrder.ENDING_SOON), eq(0), eq(20), eq(OWNER_ID)))
                .thenReturn(repoPage);

        InstallmentWalletFilter filter = new InstallmentWalletFilter(0, 20, "cc-42", InstallmentSortOrder.ENDING_SOON);
        useCase.execute(WALLET_ID, filter, OWNER_ID);

        verify(installmentRepository).findByWalletContext(
                eq(WALLET_ID), eq(EFFECTIVE_MONTH), eq("cc-42"),
                eq(InstallmentSortOrder.ENDING_SOON), eq(0), eq(20), eq(OWNER_ID));
    }

    @Test
    void execute_withEndingLateSortOrder_passesCorrectSort() {
        Wallet wallet = wallet(WALLET_ID, EFFECTIVE_MONTH);
        PageResult<Installment> repoPage = new PageResult<>(List.of(), 0, 10, 0, 0);

        when(findWalletDomainByIdBoundary.findById(WALLET_ID, OWNER_ID)).thenReturn(wallet);
        when(installmentRepository.findByWalletContext(
                eq(WALLET_ID), eq(EFFECTIVE_MONTH), isNull(),
                eq(InstallmentSortOrder.ENDING_LATE), eq(1), eq(10), eq(OWNER_ID)))
                .thenReturn(repoPage);

        InstallmentWalletFilter filter = new InstallmentWalletFilter(1, 10, null, InstallmentSortOrder.ENDING_LATE);
        useCase.execute(WALLET_ID, filter, OWNER_ID);

        verify(installmentRepository).findByWalletContext(
                eq(WALLET_ID), eq(EFFECTIVE_MONTH), isNull(),
                eq(InstallmentSortOrder.ENDING_LATE), eq(1), eq(10), eq(OWNER_ID));
    }

    @Test
    void execute_nullSortOrder_defaultsToEndingSoon() {
        Wallet wallet = wallet(WALLET_ID, EFFECTIVE_MONTH);
        PageResult<Installment> repoPage = new PageResult<>(List.of(), 0, 20, 0, 0);

        when(findWalletDomainByIdBoundary.findById(WALLET_ID, OWNER_ID)).thenReturn(wallet);
        when(installmentRepository.findByWalletContext(
                anyString(), any(), isNull(),
                eq(InstallmentSortOrder.ENDING_SOON), anyInt(), anyInt(), anyString()))
                .thenReturn(repoPage);

        // null sortOrder in filter constructor normalises to ENDING_SOON
        InstallmentWalletFilter filter = new InstallmentWalletFilter(0, 20, null, null);
        useCase.execute(WALLET_ID, filter, OWNER_ID);

        verify(installmentRepository).findByWalletContext(
                eq(WALLET_ID), eq(EFFECTIVE_MONTH), isNull(),
                eq(InstallmentSortOrder.ENDING_SOON), eq(0), eq(20), eq(OWNER_ID));
    }

    // --- helpers ---

    private static Wallet wallet(String id, YearMonth effectiveMonth) {
        return new Wallet(
                id,
                "Test wallet",
                Money.of("10000.00"),
                Money.of("10000.00"),
                LocalDate.of(effectiveMonth.getYear(), effectiveMonth.getMonthValue(), 1),
                null,
                false,
                effectiveMonth,
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );
    }

    private static Installment installment(String id, String walletId) {
        return Installment.rebuild(
                id,
                "Notebook",
                Money.of(new BigDecimal("6000.00")),
                Money.of(new BigDecimal("1000.00")),
                INSTALLMENT_NUMBER,
                LocalDate.of(2026, 3, 10),
                LAST_INSTALLMENT_DATE,
                "cc1",
                walletId,
                SOURCE_MONTH,
                false,
                null,
                FlagEnum.NONE
        );
    }
}
