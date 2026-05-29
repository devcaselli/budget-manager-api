package br.com.casellisoftware.budgetmanager.application.creditcard.usecase;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutput;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCard;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardRepository;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FindAllCreditCardsUseCaseTest {

    @Mock
    private CreditCardRepository repository;

    @InjectMocks
    private FindAllCreditCardsUseCase useCase;

    @Test
    void shouldReturnPagedCreditCards() {
        CreditCard card1 = CreditCard.create("Visa");
        CreditCard card2 = CreditCard.create("Mastercard");
        PageResult<CreditCard> pageResult = new PageResult<>(List.of(card1, card2), 0, 20, 2, 1);

        when(repository.findAll(0, 20, "owner-1")).thenReturn(pageResult);

        PageResult<CreditCardOutput> result = useCase.execute(0, 20, "owner-1");

        assertThat(result.content()).hasSize(2);
        assertThat(result.page()).isEqualTo(0);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.totalPages()).isEqualTo(1);

        verify(repository, times(1)).findAll(0, 20, "owner-1");
    }

    @Test
    void shouldReturnEmptyPageWhenNoResultsFound() {
        PageResult<CreditCard> emptyPage = new PageResult<>(List.of(), 0, 20, 0, 0);

        when(repository.findAll(0, 20, "owner-1")).thenReturn(emptyPage);

        PageResult<CreditCardOutput> result = useCase.execute(0, 20, "owner-1");

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void shouldMapDomainOutputCorrectly() {
        CreditCard card = CreditCard.create("Amex");
        String id = card.getId();
        PageResult<CreditCard> pageResult = new PageResult<>(List.of(card), 0, 10, 1, 1);

        when(repository.findAll(0, 10, "owner-1")).thenReturn(pageResult);

        PageResult<CreditCardOutput> result = useCase.execute(0, 10, "owner-1");

        assertThat(result.content().get(0)).extracting(CreditCardOutput::id, CreditCardOutput::name)
                .containsExactly(id, "Amex");
    }

    @Test
    void shouldCallRepositoryWithCorrectPaginationParams() {
        when(repository.findAll(anyInt(), anyInt(), any())).thenReturn(new PageResult<>(List.of(), 5, 50, 0, 0));

        useCase.execute(5, 50, "owner-1");

        verify(repository).findAll(5, 50, "owner-1");
    }

    @Test
    void shouldThrowExceptionWhenRepositoryThrows() {
        when(repository.findAll(anyInt(), anyInt(), any()))
                .thenThrow(new RuntimeException("Database error"));

        assertThatThrownBy(() -> useCase.execute(0, 20, "owner-1"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");
    }
}
