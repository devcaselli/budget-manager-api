package br.com.casellisoftware.budgetmanager.application.extrabudget.usecase;

import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudget;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetAllocation;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetRepository;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeleteExtraBudgetByIdUseCaseTest {

    private static final String OWNER_ID = "owner-1";
    private static final String ID = "eb-1";
    private static final String BULLET_A = "bullet-1";
    private static final String BULLET_B = "bullet-2";

    @Mock
    private ExtraBudgetRepository extraBudgetRepository;

    @Mock
    private BulletRepository bulletRepository;

    private DeleteExtraBudgetByIdUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new DeleteExtraBudgetByIdUseCase(extraBudgetRepository, bulletRepository);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ExtraBudget activeExtraBudget() {
        return ExtraBudget.rebuild(
                ID, OWNER_ID, "bonus", "wallet-1",
                Money.of("300.00"),
                List.of(
                        new ExtraBudgetAllocation(BULLET_A, Money.of("200.00")),
                        new ExtraBudgetAllocation(BULLET_B, Money.of("100.00"))
                ),
                false, null
        );
    }

    private ExtraBudget deletedExtraBudget() {
        return ExtraBudget.rebuild(
                ID, OWNER_ID, "bonus", "wallet-1",
                Money.of("300.00"),
                List.of(
                        new ExtraBudgetAllocation(BULLET_A, Money.of("200.00")),
                        new ExtraBudgetAllocation(BULLET_B, Money.of("100.00"))
                ),
                true, LocalDateTime.of(2026, 1, 1, 0, 0)
        );
    }

    private Bullet bullet(String id, Money budget, Money remaining) {
        return new Bullet(id, OWNER_ID, "desc", budget, remaining, "wallet-1", FlagEnum.NONE);
    }

    private void stubBullets() {
        when(bulletRepository.findById(BULLET_A, OWNER_ID))
                .thenReturn(Optional.of(bullet(BULLET_A, Money.of("700.00"), Money.of("500.00"))));
        when(bulletRepository.findById(BULLET_B, OWNER_ID))
                .thenReturn(Optional.of(bullet(BULLET_B, Money.of("500.00"), Money.of("300.00"))));
        when(bulletRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // -----------------------------------------------------------------------
    // Tests
    // -----------------------------------------------------------------------

    @Test
    void delete_existing_marksDeletedAndRevertsAllBullets() {
        when(extraBudgetRepository.findById(ID, OWNER_ID)).thenReturn(Optional.of(activeExtraBudget()));
        stubBullets();
        when(extraBudgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(ID, OWNER_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Bullet>> bulletCaptor = ArgumentCaptor.forClass(List.class);
        verify(bulletRepository).saveAll(bulletCaptor.capture());
        assertThat(bulletCaptor.getValue()).hasSize(2);
        ArgumentCaptor<ExtraBudget> captor = ArgumentCaptor.forClass(ExtraBudget.class);
        verify(extraBudgetRepository).save(captor.capture());
        assertThat(captor.getValue().isDeleted()).isTrue();
        assertThat(captor.getValue().getDeletedAt()).isNotNull();
    }

    @Test
    void delete_alreadyDeleted_isIdempotent_noSavesCalled() {
        when(extraBudgetRepository.findById(ID, OWNER_ID)).thenReturn(Optional.of(deletedExtraBudget()));

        useCase.execute(ID, OWNER_ID);

        verify(bulletRepository, never()).findById(any(), any());
        verify(bulletRepository, never()).saveAll(any());
        verify(extraBudgetRepository, never()).save(any());
    }

    @Test
    void delete_notFound_throwsExtraBudgetNotFoundException() {
        when(extraBudgetRepository.findById(ID, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(ID, OWNER_ID))
                .isInstanceOf(ExtraBudgetNotFoundException.class);

        verify(bulletRepository, never()).saveAll(any());
        verify(extraBudgetRepository, never()).save(any());
    }

    @Test
    void delete_bulletInsufficientRemaining_throwsAndPersistsNothing() {
        when(extraBudgetRepository.findById(ID, OWNER_ID)).thenReturn(Optional.of(activeExtraBudget()));
        // BULLET_A has remaining=50, allocation=200 → debitBy throws
        when(bulletRepository.findById(BULLET_A, OWNER_ID))
                .thenReturn(Optional.of(bullet(BULLET_A, Money.of("700.00"), Money.of("50.00"))));

        assertThatThrownBy(() -> useCase.execute(ID, OWNER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(BULLET_A);

        verify(bulletRepository, never()).saveAll(any());
        verify(extraBudgetRepository, never()).save(any());
    }

    @Test
    void delete_correctDebitValuesAppliedToBullets() {
        when(extraBudgetRepository.findById(ID, OWNER_ID)).thenReturn(Optional.of(activeExtraBudget()));
        stubBullets();
        when(extraBudgetRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(ID, OWNER_ID);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Bullet>> captor = ArgumentCaptor.forClass(List.class);
        verify(bulletRepository).saveAll(captor.capture());
        List<Bullet> saved = captor.getValue();

        Bullet savedA = saved.stream().filter(b -> b.getId().equals(BULLET_A)).findFirst().orElseThrow();
        Bullet savedB = saved.stream().filter(b -> b.getId().equals(BULLET_B)).findFirst().orElseThrow();

        // BULLET_A: budget 700-200=500, remaining 500-200=300
        assertThat(savedA.getBudget()).isEqualTo(Money.of("500.00"));
        assertThat(savedA.getRemaining()).isEqualTo(Money.of("300.00"));
        // BULLET_B: budget 500-100=400, remaining 300-100=200
        assertThat(savedB.getBudget()).isEqualTo(Money.of("400.00"));
        assertThat(savedB.getRemaining()).isEqualTo(Money.of("200.00"));
    }

    @Test
    void delete_bulletMissing_throwsBulletNotFoundException_andPersistsNothing() {
        when(extraBudgetRepository.findById(ID, OWNER_ID)).thenReturn(Optional.of(activeExtraBudget()));
        when(bulletRepository.findById(BULLET_A, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(ID, OWNER_ID))
                .isInstanceOf(BulletNotFoundException.class);

        verify(bulletRepository, never()).saveAll(any());
        verify(extraBudgetRepository, never()).save(any());
    }
}
