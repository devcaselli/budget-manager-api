package br.com.casellisoftware.budgetmanager.application.payer.usecase;

import br.com.casellisoftware.budgetmanager.domain.payer.PayerRepository;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeletePayerByIdUseCaseTest {

    @Test
    void execute_delegatesSoftDeleteToRepository() {
        PayerRepository repository = mock(PayerRepository.class);
        ShareRepository shareRepository = mock(ShareRepository.class);
        when(shareRepository.findActiveByPayerId("payer-1", "owner-1")).thenReturn(List.of());
        DeletePayerByIdUseCase useCase = new DeletePayerByIdUseCase(repository, shareRepository);

        useCase.execute("payer-1", "owner-1");

        verify(repository).deleteById("payer-1", "owner-1");
    }
}
