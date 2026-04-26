package br.com.casellisoftware.budgetmanager.application.bullet.usecase;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.domain.bullet.Bullet;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatchBulletUseCaseTest {

    @Mock
    private BulletRepository bulletRepository;

    @InjectMocks
    private PatchBulletUseCase useCase;

    @Test
    void execute_patchesOnlyFieldsPresentInContract() {
        Bullet existing = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1",
                "groceries",
                new BigDecimal("650.00"),
                new BigDecimal("410.00"),
                "wallet-1"
        );

        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(existing));
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));

        BulletOutput output = useCase.execute(input);

        ArgumentCaptor<Bullet> captor = ArgumentCaptor.forClass(Bullet.class);
        verify(bulletRepository).save(captor.capture());
        Bullet saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo("bullet-1");
        assertThat(saved.getWalletId()).isEqualTo("wallet-1");
        assertThat(saved.getDescription()).isEqualTo("groceries");
        assertThat(saved.getBudget()).isEqualTo(Money.of("650.00"));
        assertThat(saved.getRemaining()).isEqualTo(Money.of("410.00"));

        assertThat(output.id()).isEqualTo(saved.getId());
        assertThat(output.description()).isEqualTo(saved.getDescription());
        assertThat(output.budget()).isEqualByComparingTo("650.00");
        assertThat(output.remaining()).isEqualByComparingTo("410.00");
        assertThat(output.walletId()).isEqualTo(saved.getWalletId());
    }

    @Test
    void execute_whenOnlyDescriptionProvided_preservesFinancialFields() {
        Bullet existing = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1",
                "groceries",
                null,
                null,
                null
        );

        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(existing));
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));

        useCase.execute(input);

        ArgumentCaptor<Bullet> captor = ArgumentCaptor.forClass(Bullet.class);
        verify(bulletRepository).save(captor.capture());
        Bullet saved = captor.getValue();

        assertThat(saved.getDescription()).isEqualTo("groceries");
        assertThat(saved.getBudget()).isEqualTo(Money.of("500.00"));
        assertThat(saved.getRemaining()).isEqualTo(Money.of("320.00"));
        assertThat(saved.getWalletId()).isEqualTo("wallet-1");
    }

    @Test
    void execute_whenBulletDoesNotExist_throwsAndDoesNotSave() {
        PatchBulletInput input = new PatchBulletInput(
                "bullet-missing",
                "groceries",
                null,
                null,
                null
        );
        when(bulletRepository.findById("bullet-missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(BulletNotFoundException.class)
                .hasMessageContaining("bullet-missing");

        verify(bulletRepository, never()).save(any());
    }

    @Test
    void execute_whenPatchIsEmpty_savesCurrentStateUnchanged() {
        Bullet existing = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1",
                null,
                null,
                null,
                null
        );

        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(existing));
        when(bulletRepository.save(any(Bullet.class))).thenAnswer(inv -> inv.getArgument(0));

        BulletOutput output = useCase.execute(input);

        ArgumentCaptor<Bullet> captor = ArgumentCaptor.forClass(Bullet.class);
        verify(bulletRepository).save(captor.capture());
        assertThat(captor.getValue()).isSameAs(existing);
        assertThat(output.id()).isEqualTo("bullet-1");
        assertThat(output.description()).isEqualTo("rent");
        assertThat(output.budget()).isEqualByComparingTo("500.00");
        assertThat(output.remaining()).isEqualByComparingTo("320.00");
        assertThat(output.walletId()).isEqualTo("wallet-1");
    }

    @Test
    void execute_whenWalletIdChanges_throwsAndDoesNotSave() {
        Bullet existing = new Bullet(
                "bullet-1",
                "rent",
                Money.of("500.00"),
                Money.of("320.00"),
                "wallet-1"
        );
        PatchBulletInput input = new PatchBulletInput(
                "bullet-1",
                null,
                null,
                null,
                "wallet-2"
        );

        when(bulletRepository.findById("bullet-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> useCase.execute(input))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("walletId is immutable");

        verify(bulletRepository, never()).save(any());
    }
}
