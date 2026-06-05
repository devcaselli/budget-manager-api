package br.com.casellisoftware.budgetmanager.application.sharing.usecase;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletDomainByIdBoundary;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.sharing.Share;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.wallet.Wallet;
import br.com.casellisoftware.budgetmanager.domain.wallet.WalletState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StopWalletShareUseCaseTest {

    private static final String OWNER = "owner-1";
    private static final String WALLET_ID = "wallet-jun";
    private static final String SHARE_ID = "share-1";

    @Mock
    private ShareRepository shareRepository;
    @Mock
    private FindWalletDomainByIdBoundary findWalletDomainByIdBoundary;

    private StopWalletShareUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new StopWalletShareUseCase(shareRepository, findWalletDomainByIdBoundary);
    }

    @Test
    void execute_stopsShareFromWalletMonth() {
        Wallet junWallet = wallet(YearMonth.of(2026, 6));
        when(findWalletDomainByIdBoundary.findById(WALLET_ID, OWNER)).thenReturn(junWallet);
        when(shareRepository.findById(SHARE_ID, OWNER)).thenReturn(Optional.of(subscriptionShare()));

        useCase.execute(WALLET_ID, SHARE_ID, OWNER);

        ArgumentCaptor<Share> captor = ArgumentCaptor.forClass(Share.class);
        verify(shareRepository).save(captor.capture());
        assertThat(captor.getValue().getStoppedFromMonth()).isEqualTo(YearMonth.of(2026, 6));
    }

    @Test
    void execute_whenShareMissing_throws() {
        when(findWalletDomainByIdBoundary.findById(WALLET_ID, OWNER)).thenReturn(wallet(YearMonth.of(2026, 6)));
        when(shareRepository.findById(SHARE_ID, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(WALLET_ID, SHARE_ID, OWNER))
                .isInstanceOf(ShareNotFoundException.class);
    }

    private static Wallet wallet(YearMonth month) {
        return new Wallet(
                WALLET_ID,
                "wallet",
                Money.of("1000.00"),
                Money.of("1000.00"),
                LocalDate.of(2026, 5, 1),
                null,
                false,
                month,
                WalletState.PRODUCTION,
                FlagEnum.NONE
        );
    }

    private static Share subscriptionShare() {
        return Share.create(
                "wallet-maio",
                ShareSourceType.SUBSCRIPTION,
                "sub-1",
                Money.of("100.00"),
                Money.of("50.00"),
                List.of(new Share.ShareQuotaAllocation("payer-1", Money.of("50.00"))),
                OWNER,
                Instant.parse("2026-05-01T00:00:00Z")
        );
    }
}
