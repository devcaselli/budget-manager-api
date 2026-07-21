package br.com.casellisoftware.budgetmanager.application.pluggy.usecase;

import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyItemStatusOutput;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnection;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionRepository;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPluggyItemStatusUseCaseTest {

    private static final String OWNER = "owner-1";
    private static final String ITEM_ID = "item-1";

    @Mock
    private PluggyClient pluggyClient;
    @Mock
    private PluggyConnectionRepository pluggyConnectionRepository;

    private GetPluggyItemStatusUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetPluggyItemStatusUseCase(pluggyClient, pluggyConnectionRepository);
    }

    @Test
    void execute_ownedItem_returnsStatusFromPluggy() {
        PluggyConnection owned = new PluggyConnection(
                "conn-1", OWNER, ITEM_ID, "201", "UPDATING", List.of("acc-1"), Instant.now(), Instant.now());
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER)).thenReturn(Optional.of(owned));
        when(pluggyClient.getItem(ITEM_ID)).thenReturn(new PluggyItem(ITEM_ID, "201", "UPDATED"));

        PluggyItemStatusOutput output = useCase.execute(OWNER, ITEM_ID);

        assertThat(output.status()).isEqualTo("UPDATED");
    }

    @Test
    void execute_itemNotOwnedByCaller_throwsNotFound_andNeverCallsPluggy() {
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(OWNER, ITEM_ID))
                .isInstanceOf(PluggyConnectionNotFoundException.class);

        verify(pluggyClient, never()).getItem(ITEM_ID);
    }
}
