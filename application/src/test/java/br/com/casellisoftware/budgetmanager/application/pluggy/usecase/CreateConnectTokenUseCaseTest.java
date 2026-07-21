package br.com.casellisoftware.budgetmanager.application.pluggy.usecase;

import br.com.casellisoftware.budgetmanager.application.pluggy.dto.ConnectTokenOutput;
import br.com.casellisoftware.budgetmanager.domain.pluggy.ConnectToken;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnection;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionRepository;
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
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateConnectTokenUseCaseTest {

    private static final String OWNER = "owner-1";
    private static final String ITEM_ID = "item-1";

    @Mock
    private PluggyClient pluggyClient;
    @Mock
    private PluggyConnectionRepository pluggyConnectionRepository;

    private CreateConnectTokenUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new CreateConnectTokenUseCase(pluggyClient, pluggyConnectionRepository);
    }

    @Test
    void execute_noItemId_newConnectionFlow_skipsOwnershipCheck() {
        when(pluggyClient.createConnectToken(OWNER, null)).thenReturn(new ConnectToken("connect-token-xyz"));

        ConnectTokenOutput output = useCase.execute(OWNER, null);

        assertThat(output.connectToken()).isEqualTo("connect-token-xyz");
        verifyNoInteractions(pluggyConnectionRepository);
    }

    @Test
    void execute_singleArgOverload_delegatesToNullItemId() {
        when(pluggyClient.createConnectToken(OWNER, null)).thenReturn(new ConnectToken("connect-token-xyz"));

        ConnectTokenOutput output = useCase.execute(OWNER);

        assertThat(output.connectToken()).isEqualTo("connect-token-xyz");
        verifyNoInteractions(pluggyConnectionRepository);
    }

    @Test
    void execute_itemIdOwnedByCaller_returnsUpdateModeToken() {
        PluggyConnection owned = new PluggyConnection(
                "conn-1", OWNER, ITEM_ID, "201", "UPDATED", List.of("acc-1"), Instant.now(), Instant.now());
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER)).thenReturn(Optional.of(owned));
        when(pluggyClient.createConnectToken(OWNER, ITEM_ID)).thenReturn(new ConnectToken("update-token-xyz"));

        ConnectTokenOutput output = useCase.execute(OWNER, ITEM_ID);

        assertThat(output.connectToken()).isEqualTo("update-token-xyz");
    }

    @Test
    void execute_itemIdNotOwnedByCaller_throwsNotFound_andNeverCallsPluggy() {
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(OWNER, ITEM_ID))
                .isInstanceOf(PluggyConnectionNotFoundException.class);

        verify(pluggyClient, never()).createConnectToken(isNull(), isNull());
        verify(pluggyClient, never()).createConnectToken(OWNER, ITEM_ID);
    }
}
