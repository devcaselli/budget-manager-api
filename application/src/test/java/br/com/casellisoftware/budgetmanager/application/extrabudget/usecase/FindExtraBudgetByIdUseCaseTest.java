package br.com.casellisoftware.budgetmanager.application.extrabudget.usecase;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetOutput;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudget;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetAllocation;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindExtraBudgetByIdUseCaseTest {

    private static final String OWNER_ID = "owner-1";
    private static final String ID = "eb-1";

    @Mock
    private ExtraBudgetRepository extraBudgetRepository;

    private FindExtraBudgetByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new FindExtraBudgetByIdUseCase(extraBudgetRepository);
    }

    private ExtraBudget sampleExtraBudget(String ownerId) {
        return ExtraBudget.rebuild(
                ID, ownerId, "bonus", "wallet-1",
                Money.of("300.00"),
                List.of(
                        new ExtraBudgetAllocation("bullet-1", Money.of("200.00")),
                        new ExtraBudgetAllocation("bullet-2", Money.of("100.00"))
                ),
                false, null
        );
    }

    @Test
    void find_existing_returnsOutput() {
        ExtraBudget eb = sampleExtraBudget(OWNER_ID);
        when(extraBudgetRepository.findById(ID, OWNER_ID)).thenReturn(Optional.of(eb));

        ExtraBudgetOutput output = useCase.execute(ID, OWNER_ID);

        assertThat(output.id()).isEqualTo(ID);
        assertThat(output.description()).isEqualTo("bonus");
        assertThat(output.walletId()).isEqualTo("wallet-1");
        assertThat(output.allocations()).hasSize(2);
    }

    @Test
    void find_notFound_throwsExtraBudgetNotFoundException() {
        when(extraBudgetRepository.findById(ID, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(ID, OWNER_ID))
                .isInstanceOf(ExtraBudgetNotFoundException.class);
    }

    @Test
    void find_existingButOtherOwner_throwsNotFound() {
        // findById(id, ownerId) delegates to findById(id).filter(ownerId match)
        // If ownerId differs, filter returns empty → NotFoundException
        when(extraBudgetRepository.findById(ID, "other-owner")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(ID, "other-owner"))
                .isInstanceOf(ExtraBudgetNotFoundException.class);
    }

    @Test
    void find_softDeleted_throwsNotFound() {
        ExtraBudget deleted = ExtraBudget.rebuild(
                ID, OWNER_ID, "bonus", "wallet-1",
                Money.of("300.00"),
                List.of(
                        new ExtraBudgetAllocation("bullet-1", Money.of("200.00")),
                        new ExtraBudgetAllocation("bullet-2", Money.of("100.00"))
                ),
                true, LocalDateTime.of(2026, 1, 1, 0, 0)
        );
        when(extraBudgetRepository.findById(ID, OWNER_ID)).thenReturn(Optional.of(deleted));

        assertThatThrownBy(() -> useCase.execute(ID, OWNER_ID))
                .isInstanceOf(ExtraBudgetNotFoundException.class);
    }
}
