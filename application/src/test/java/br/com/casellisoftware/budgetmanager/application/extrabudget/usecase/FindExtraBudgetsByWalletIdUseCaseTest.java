package br.com.casellisoftware.budgetmanager.application.extrabudget.usecase;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetOutput;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudget;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetAllocation;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindExtraBudgetsByWalletIdUseCaseTest {

    private static final String OWNER_ID = "owner-1";
    private static final String WALLET_ID = "wallet-1";

    @Mock
    private ExtraBudgetRepository extraBudgetRepository;

    private FindExtraBudgetsByWalletIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindExtraBudgetsByWalletIdUseCase(extraBudgetRepository);
    }

    private ExtraBudget extraBudget(String id, String ownerId) {
        return ExtraBudget.rebuild(
                id, ownerId, "bonus", WALLET_ID,
                Money.of("200.00"),
                List.of(new ExtraBudgetAllocation("bullet-1", Money.of("200.00"))),
                false, null
        );
    }

    @Test
    void find_byWalletId_returnsList() {
        List<ExtraBudget> stored = List.of(
                extraBudget("eb-1", OWNER_ID),
                extraBudget("eb-2", OWNER_ID)
        );
        when(extraBudgetRepository.findByWalletId(WALLET_ID, OWNER_ID)).thenReturn(stored);

        List<ExtraBudgetOutput> result = useCase.execute(WALLET_ID, OWNER_ID);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ExtraBudgetOutput::walletId).containsOnly(WALLET_ID);
    }

    @Test
    void find_byWalletId_emptyReturnsEmptyList() {
        when(extraBudgetRepository.findByWalletId(WALLET_ID, OWNER_ID)).thenReturn(List.of());

        List<ExtraBudgetOutput> result = useCase.execute(WALLET_ID, OWNER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void find_byWalletId_filtersByOwnerId() {
        // Repository already filters by ownerId at query level; use case passes it through
        when(extraBudgetRepository.findByWalletId(WALLET_ID, OWNER_ID))
                .thenReturn(List.of(extraBudget("eb-1", OWNER_ID)));
        when(extraBudgetRepository.findByWalletId(WALLET_ID, "other-owner"))
                .thenReturn(List.of());

        List<ExtraBudgetOutput> ownerResult = useCase.execute(WALLET_ID, OWNER_ID);
        List<ExtraBudgetOutput> otherResult = useCase.execute(WALLET_ID, "other-owner");

        assertThat(ownerResult).hasSize(1);
        assertThat(otherResult).isEmpty();
    }
}
