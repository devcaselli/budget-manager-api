package br.com.casellisoftware.budgetmanager.application.pluggy.usecase;

import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyConnectionOutput;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyAccount;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyClient;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnection;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionRepository;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterPluggyItemUseCaseTest {

    private static final String OWNER = "owner-1";
    private static final String ITEM_ID = "item-1";
    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-05-19T15:00:00Z"), ZoneId.of("UTC"));

    @Mock
    private PluggyClient pluggyClient;
    @Mock
    private PluggyConnectionRepository pluggyConnectionRepository;

    private RegisterPluggyItemUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new RegisterPluggyItemUseCase(pluggyClient, pluggyConnectionRepository, FIXED_CLOCK);
    }

    @Test
    void execute_newConnection_createsAndSaves() {
        when(pluggyClient.getItem(ITEM_ID)).thenReturn(new PluggyItem(ITEM_ID, "201", "UPDATED"));
        when(pluggyClient.listAccounts(ITEM_ID)).thenReturn(List.of(
                new PluggyAccount("acc-1", ITEM_ID, "Conta Corrente", "BANK")));
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER)).thenReturn(Optional.empty());
        when(pluggyConnectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PluggyConnectionOutput output = useCase.execute(OWNER, ITEM_ID);

        assertThat(output.itemId()).isEqualTo(ITEM_ID);
        assertThat(output.connectorId()).isEqualTo("201");
        assertThat(output.status()).isEqualTo("UPDATED");
        assertThat(output.accountIds()).containsExactly("acc-1");

        ArgumentCaptor<PluggyConnection> captor = ArgumentCaptor.forClass(PluggyConnection.class);
        verify(pluggyConnectionRepository).save(captor.capture());
        assertThat(captor.getValue().getOwnerId()).isEqualTo(OWNER);
        assertThat(captor.getValue().getCreatedAt()).isEqualTo(FIXED_CLOCK.instant());
    }

    @Test
    void execute_existingConnection_upsertsAccountsAndStatus_preservesCreatedAt() {
        Instant originalCreatedAt = Instant.parse("2026-01-01T00:00:00Z");
        PluggyConnection existing = new PluggyConnection(
                "conn-1", OWNER, ITEM_ID, "201", "UPDATING", List.of("acc-old"), originalCreatedAt, originalCreatedAt);

        when(pluggyClient.getItem(ITEM_ID)).thenReturn(new PluggyItem(ITEM_ID, "201", "UPDATED"));
        when(pluggyClient.listAccounts(ITEM_ID)).thenReturn(List.of(
                new PluggyAccount("acc-1", ITEM_ID, "Conta Corrente", "BANK"),
                new PluggyAccount("acc-2", ITEM_ID, "Cartão", "CREDIT")));
        when(pluggyConnectionRepository.findByItemIdAndOwnerId(ITEM_ID, OWNER)).thenReturn(Optional.of(existing));
        when(pluggyConnectionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PluggyConnectionOutput output = useCase.execute(OWNER, ITEM_ID);

        assertThat(output.id()).isEqualTo("conn-1");
        assertThat(output.status()).isEqualTo("UPDATED");
        assertThat(output.accountIds()).containsExactly("acc-1", "acc-2");
        assertThat(output.createdAt()).isEqualTo(originalCreatedAt);
        assertThat(output.updatedAt()).isEqualTo(FIXED_CLOCK.instant());
    }
}
