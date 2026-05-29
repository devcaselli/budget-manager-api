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
class FindExtraBudgetsByBulletIdUseCaseTest {

    private static final String OWNER_ID = "owner-1";
    private static final String BULLET_ID = "bullet-1";

    @Mock
    private ExtraBudgetRepository extraBudgetRepository;

    private FindExtraBudgetsByBulletIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindExtraBudgetsByBulletIdUseCase(extraBudgetRepository);
    }

    private ExtraBudget extraBudget(String id, String ownerId) {
        return ExtraBudget.rebuild(
                id, ownerId, "bonus", "wallet-1",
                Money.of("200.00"),
                List.of(new ExtraBudgetAllocation(BULLET_ID, Money.of("200.00"))),
                false, null
        );
    }

    @Test
    void find_byBulletId_returnsList() {
        List<ExtraBudget> stored = List.of(
                extraBudget("eb-1", OWNER_ID),
                extraBudget("eb-2", OWNER_ID)
        );
        when(extraBudgetRepository.findByBulletId(BULLET_ID, OWNER_ID)).thenReturn(stored);

        List<ExtraBudgetOutput> result = useCase.execute(BULLET_ID, OWNER_ID);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(o ->
                assertThat(o.allocations()).extracting(a -> a.bulletId()).contains(BULLET_ID));
    }

    @Test
    void find_byBulletId_emptyReturnsEmptyList() {
        when(extraBudgetRepository.findByBulletId(BULLET_ID, OWNER_ID)).thenReturn(List.of());

        List<ExtraBudgetOutput> result = useCase.execute(BULLET_ID, OWNER_ID);

        assertThat(result).isEmpty();
    }

    @Test
    void find_byBulletId_filtersByOwnerId() {
        when(extraBudgetRepository.findByBulletId(BULLET_ID, OWNER_ID))
                .thenReturn(List.of(extraBudget("eb-1", OWNER_ID)));
        when(extraBudgetRepository.findByBulletId(BULLET_ID, "other-owner"))
                .thenReturn(List.of());

        List<ExtraBudgetOutput> ownerResult = useCase.execute(BULLET_ID, OWNER_ID);
        List<ExtraBudgetOutput> otherResult = useCase.execute(BULLET_ID, "other-owner");

        assertThat(ownerResult).hasSize(1);
        assertThat(otherResult).isEmpty();
    }
}
