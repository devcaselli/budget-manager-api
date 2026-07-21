package br.com.casellisoftware.budgetmanager.rest.pluggy;

import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.CreateConnectTokenBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.FindPluggyConnectionsBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.FindPluggyTransactionsBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.GetPluggyItemStatusBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.MaterializePluggyTransactionsBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.boundary.RegisterPluggyItemBoundary;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.ConnectTokenOutput;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyConnectionOutput;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyItemStatusOutput;
import br.com.casellisoftware.budgetmanager.application.pluggy.dto.PluggyTransactionPreviewOutput;
import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;
import br.com.casellisoftware.budgetmanager.domain.pluggy.PluggyConnectionNotFoundException;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} for {@link PluggyConnectController}. Validates HTTP contract:
 * status codes, JSON body, and validation via {@link GlobalExceptionHandler}.
 *
 * <p>Security is disabled via {@code app.security.enabled=false} in
 * {@code infra/src/test/resources/application.yaml}. {@code AuthenticatedUser}
 * resolves to {@code LEGACY_OWNER_ID}.</p>
 */
@WebMvcTest(controllers = PluggyConnectController.class)
@Import(GlobalExceptionHandler.class)
class PluggyConnectControllerTest {

    private static final String BASE_PATH = "/pluggy";
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CreateConnectTokenBoundary createConnectTokenBoundary;

    @MockitoBean
    private RegisterPluggyItemBoundary registerPluggyItemBoundary;

    @MockitoBean
    private FindPluggyConnectionsBoundary findPluggyConnectionsBoundary;

    @MockitoBean
    private FindPluggyTransactionsBoundary findPluggyTransactionsBoundary;

    @MockitoBean
    private MaterializePluggyTransactionsBoundary materializePluggyTransactionsBoundary;

    @MockitoBean
    private GetPluggyItemStatusBoundary getPluggyItemStatusBoundary;

    // ---------- POST /pluggy/connect-token ----------

    @Test
    void createConnectToken_noBody_newConnectionFlow_returns200() throws Exception {
        when(createConnectTokenBoundary.execute(anyString(), isNull()))
                .thenReturn(new ConnectTokenOutput("connect-token-xyz"));

        mockMvc.perform(post(BASE_PATH + "/connect-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectToken").value("connect-token-xyz"));
    }

    @Test
    void createConnectToken_nullItemIdInBody_newConnectionFlow_returns200() throws Exception {
        when(createConnectTokenBoundary.execute(anyString(), isNull()))
                .thenReturn(new ConnectTokenOutput("connect-token-xyz"));

        mockMvc.perform(post(BASE_PATH + "/connect-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectToken").value("connect-token-xyz"));
    }

    @Test
    void createConnectToken_ownedItemId_updateModeToken_returns200() throws Exception {
        when(createConnectTokenBoundary.execute(anyString(), eq("item-1")))
                .thenReturn(new ConnectTokenOutput("update-token-xyz"));

        mockMvc.perform(post(BASE_PATH + "/connect-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": \"item-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectToken").value("update-token-xyz"));
    }

    @Test
    void createConnectToken_notOwnedItemId_returns404() throws Exception {
        when(createConnectTokenBoundary.execute(anyString(), eq("item-unknown")))
                .thenThrow(new PluggyConnectionNotFoundException("item-unknown", "owner-x"));

        mockMvc.perform(post(BASE_PATH + "/connect-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": \"item-unknown\"}"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    // ---------- POST /pluggy/items ----------

    @Test
    void registerItem_happyPath_returns200WithConnection() throws Exception {
        when(registerPluggyItemBoundary.execute(anyString(), eq("item-1")))
                .thenReturn(new PluggyConnectionOutput(
                        "conn-1", "item-1", "201", "UPDATED", List.of("acc-1"),
                        Instant.parse("2026-05-01T00:00:00Z"), Instant.parse("2026-05-01T00:00:00Z")));

        mockMvc.perform(post(BASE_PATH + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": \"item-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("conn-1"))
                .andExpect(jsonPath("$.itemId").value("item-1"))
                .andExpect(jsonPath("$.status").value("UPDATED"))
                .andExpect(jsonPath("$.accountIds[0]").value("acc-1"));
    }

    @Test
    void registerItem_blankItemId_returns400() throws Exception {
        mockMvc.perform(post(BASE_PATH + "/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    // ---------- GET /pluggy/connections ----------

    @Test
    void listConnections_returnsOwnerConnections() throws Exception {
        when(findPluggyConnectionsBoundary.execute(anyString())).thenReturn(List.of(
                new PluggyConnectionOutput("conn-1", "item-1", "201", "UPDATED", List.of("acc-1"),
                        Instant.parse("2026-05-01T00:00:00Z"), Instant.parse("2026-05-01T00:00:00Z"))));

        mockMvc.perform(get(BASE_PATH + "/connections"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("conn-1"))
                .andExpect(jsonPath("$[0].itemId").value("item-1"));
    }

    @Test
    void listConnections_noConnections_returnsEmptyArray() throws Exception {
        when(findPluggyConnectionsBoundary.execute(anyString())).thenReturn(List.of());

        mockMvc.perform(get(BASE_PATH + "/connections"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // ---------- GET /pluggy/items/{itemId}/transactions ----------

    @Test
    void previewTransactions_happyPath_returns200WithPreviewList() throws Exception {
        when(findPluggyTransactionsBoundary.execute(anyString(), eq("item-1"), any(), any()))
                .thenReturn(List.of(new PluggyTransactionPreviewOutput(
                        "tx-1", "acc-1", "Uber", BigDecimal.valueOf(-45.90), "BRL",
                        LocalDate.of(2026, 5, 10), true, false)));

        mockMvc.perform(get(BASE_PATH + "/items/item-1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("tx-1"))
                .andExpect(jsonPath("$[0].isExpense").value(true))
                .andExpect(jsonPath("$[0].alreadyImported").value(false));
    }

    @Test
    void previewTransactions_unknownConnection_returns404() throws Exception {
        when(findPluggyTransactionsBoundary.execute(anyString(), eq("item-unknown"), any(), any()))
                .thenThrow(new PluggyConnectionNotFoundException("item-unknown", "owner-x"));

        mockMvc.perform(get(BASE_PATH + "/items/item-unknown/transactions"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void previewTransactions_withFromAndTo_passesDatesThrough() throws Exception {
        when(findPluggyTransactionsBoundary.execute(anyString(), eq("item-1"),
                eq(LocalDate.of(2026, 5, 1)), eq(LocalDate.of(2026, 5, 31))))
                .thenReturn(List.of());

        mockMvc.perform(get(BASE_PATH + "/items/item-1/transactions")
                        .param("from", "2026-05-01")
                        .param("to", "2026-05-31"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    // ---------- POST /pluggy/items/{itemId}/materialize ----------

    @Test
    void materializeTransactions_all_returns200WithReport() throws Exception {
        when(materializePluggyTransactionsBoundary.execute(anyString(), eq("item-1"), isNull(), eq(true)))
                .thenReturn(new SyncReport(3, 1, 1, 0));

        mockMvc.perform(post(BASE_PATH + "/items/item-1/materialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"all\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(3))
                .andExpect(jsonPath("$.skipped").value(1))
                .andExpect(jsonPath("$.fallback").value(1))
                .andExpect(jsonPath("$.errors").value(0));
    }

    @Test
    void materializeTransactions_selectedIds_returns200WithReport() throws Exception {
        when(materializePluggyTransactionsBoundary.execute(
                anyString(), eq("item-1"), eq(List.of("tx-1", "tx-2")), eq(false)))
                .thenReturn(new SyncReport(2, 0, 0, 0));

        mockMvc.perform(post(BASE_PATH + "/items/item-1/materialize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"transactionIds\": [\"tx-1\", \"tx-2\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2));
    }

    // ---------- GET /pluggy/items/{itemId}/status ----------

    @Test
    void getItemStatus_ownedItem_returns200WithStatus() throws Exception {
        when(getPluggyItemStatusBoundary.execute(anyString(), eq("item-1")))
                .thenReturn(new PluggyItemStatusOutput("UPDATED"));

        mockMvc.perform(get(BASE_PATH + "/items/item-1/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UPDATED"));
    }

    @Test
    void getItemStatus_notOwnedItem_returns404() throws Exception {
        when(getPluggyItemStatusBoundary.execute(anyString(), eq("item-unknown")))
                .thenThrow(new PluggyConnectionNotFoundException("item-unknown", "owner-x"));

        mockMvc.perform(get(BASE_PATH + "/items/item-unknown/status"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }
}
