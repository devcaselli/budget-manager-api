package br.com.casellisoftware.budgetmanager.rest.extrabudget;

import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.DeleteExtraBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetInput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.ExtraBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.FindExtraBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.FindExtraBudgetsByBulletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.FindExtraBudgetsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.SaveExtraBudgetBoundary;
import br.com.casellisoftware.budgetmanager.domain.extrabudget.ExtraBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos.AllocationRequestDto;
import br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos.AllocationResponseDto;
import br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos.ExtraBudgetRequestDto;
import br.com.casellisoftware.budgetmanager.rest.extrabudget.dtos.ExtraBudgetResponseDto;
import br.com.casellisoftware.budgetmanager.rest.extrabudget.mappers.ExtraBudgetRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} for {@link ExtraBudgetController}. Validates HTTP contract:
 * status codes, headers, JSON body, validation, and error handling via
 * {@link GlobalExceptionHandler}.
 */
@WebMvcTest(controllers = ExtraBudgetController.class)
@Import(GlobalExceptionHandler.class)
class ExtraBudgetControllerTest {

    private static final String BASE_PATH = "/extra-budgets";
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SaveExtraBudgetBoundary saveExtraBudgetBoundary;

    @MockitoBean
    private FindExtraBudgetByIdBoundary findExtraBudgetByIdBoundary;

    @MockitoBean
    private FindExtraBudgetsByWalletIdBoundary findExtraBudgetsByWalletIdBoundary;

    @MockitoBean
    private FindExtraBudgetsByBulletIdBoundary findExtraBudgetsByBulletIdBoundary;

    @MockitoBean
    private DeleteExtraBudgetByIdBoundary deleteExtraBudgetByIdBoundary;

    @MockitoBean
    private ExtraBudgetRestMapper mapper;

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private ExtraBudgetRequestDto validRequest() {
        return new ExtraBudgetRequestDto(
                "bonus", "wallet-1",
                new BigDecimal("300.00"),
                List.of(
                        new AllocationRequestDto("bullet-1", new BigDecimal("200.00")),
                        new AllocationRequestDto("bullet-2", new BigDecimal("100.00"))
                )
        );
    }

    private ExtraBudgetOutput sampleOutput(String id) {
        return new ExtraBudgetOutput(
                id, "legacy", "bonus", "wallet-1",
                new BigDecimal("300.00"), "BRL",
                List.of(
                        new br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.AllocationOutput("bullet-1", new BigDecimal("200.00")),
                        new br.com.casellisoftware.budgetmanager.application.extrabudget.boundary.AllocationOutput("bullet-2", new BigDecimal("100.00"))
                ),
                false, null
        );
    }

    private ExtraBudgetResponseDto sampleResponse(String id) {
        return new ExtraBudgetResponseDto(
                id, "bonus", "wallet-1",
                new BigDecimal("300.00"), "BRL",
                List.of(
                        new AllocationResponseDto("bullet-1", new BigDecimal("200.00")),
                        new AllocationResponseDto("bullet-2", new BigDecimal("100.00"))
                ),
                false, null
        );
    }

    // -----------------------------------------------------------------------
    // POST — Happy path
    // -----------------------------------------------------------------------

    @Test
    void POST_validPayload_returns201WithLocationAndBody() throws Exception {
        ExtraBudgetRequestDto request = validRequest();
        ExtraBudgetInput input = new ExtraBudgetInput("bonus", "wallet-1", new BigDecimal("300.00"), List.of(), "legacy");
        ExtraBudgetOutput output = sampleOutput("eb-42");
        ExtraBudgetResponseDto response = sampleResponse("eb-42");

        when(mapper.toInput(any(ExtraBudgetRequestDto.class))).thenReturn(input);
        when(saveExtraBudgetBoundary.execute(any())).thenReturn(output);
        when(mapper.toResponse(output)).thenReturn(response);

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/extra-budgets/eb-42")))
                .andExpect(jsonPath("$.id").value("eb-42"))
                .andExpect(jsonPath("$.description").value("bonus"))
                .andExpect(jsonPath("$.walletId").value("wallet-1"))
                .andExpect(jsonPath("$.allocations", hasSize(2)));
    }

    // -----------------------------------------------------------------------
    // POST — Validation
    // -----------------------------------------------------------------------

    @Test
    void POST_blankDescription_returns400() throws Exception {
        String body = """
                {
                    "description": "",
                    "walletId": "wallet-1",
                    "amount": 300.00,
                    "allocations": [{"bulletId": "b1", "amount": 300.00}]
                }
                """;

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[?(@.field == 'description')]").exists())
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveExtraBudgetBoundary, never()).execute(any());
    }

    @Test
    void POST_negativeAmount_returns400() throws Exception {
        String body = """
                {
                    "description": "bonus",
                    "walletId": "wallet-1",
                    "amount": -100.00,
                    "allocations": [{"bulletId": "b1", "amount": 100.00}]
                }
                """;

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'amount')]").exists())
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveExtraBudgetBoundary, never()).execute(any());
    }

    @Test
    void POST_emptyAllocations_returns400() throws Exception {
        String body = """
                {
                    "description": "bonus",
                    "walletId": "wallet-1",
                    "amount": 300.00,
                    "allocations": []
                }
                """;

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'allocations')]").exists())
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveExtraBudgetBoundary, never()).execute(any());
    }

    @Test
    void POST_allocationWithNonPositiveAmount_returns400() throws Exception {
        String body = """
                {
                    "description": "bonus",
                    "walletId": "wallet-1",
                    "amount": 300.00,
                    "allocations": [{"bulletId": "b1", "amount": -50.00}]
                }
                """;

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveExtraBudgetBoundary, never()).execute(any());
    }

    @Test
    void POST_missingWalletId_returns400() throws Exception {
        String body = """
                {
                    "description": "bonus",
                    "amount": 300.00,
                    "allocations": [{"bulletId": "b1", "amount": 300.00}]
                }
                """;

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'walletId')]").exists());

        verify(saveExtraBudgetBoundary, never()).execute(any());
    }

    @Test
    void POST_propagatesOwnerIdFromAuthenticatedUser() throws Exception {
        ExtraBudgetRequestDto request = validRequest();
        ExtraBudgetInput inputWithLegacy = new ExtraBudgetInput("bonus", "wallet-1", new BigDecimal("300.00"), List.of(), "legacy");
        ExtraBudgetOutput output = sampleOutput("eb-99");
        ExtraBudgetResponseDto response = sampleResponse("eb-99");

        when(mapper.toInput(any(ExtraBudgetRequestDto.class))).thenReturn(
                new ExtraBudgetInput("bonus", "wallet-1", new BigDecimal("300.00"), List.of(), "legacy"));
        when(saveExtraBudgetBoundary.execute(any())).thenReturn(output);
        when(mapper.toResponse(output)).thenReturn(response);

        mockMvc.perform(post(BASE_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        org.mockito.ArgumentCaptor<ExtraBudgetInput> captor =
                org.mockito.ArgumentCaptor.forClass(ExtraBudgetInput.class);
        verify(saveExtraBudgetBoundary).execute(captor.capture());
        org.assertj.core.api.Assertions.assertThat(captor.getValue().ownerId()).isEqualTo("legacy");
    }

    // -----------------------------------------------------------------------
    // GET by id
    // -----------------------------------------------------------------------

    @Test
    void GET_byId_existing_returns200() throws Exception {
        ExtraBudgetOutput output = sampleOutput("eb-1");
        ExtraBudgetResponseDto response = sampleResponse("eb-1");

        when(findExtraBudgetByIdBoundary.execute("eb-1", "legacy")).thenReturn(output);
        when(mapper.toResponse(output)).thenReturn(response);

        mockMvc.perform(get(BASE_PATH + "/eb-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("eb-1"))
                .andExpect(jsonPath("$.description").value("bonus"))
                .andExpect(jsonPath("$.allocations", hasSize(2)));
    }

    @Test
    void GET_byId_notFound_returns404() throws Exception {
        when(findExtraBudgetByIdBoundary.execute("nonexistent", "legacy"))
                .thenThrow(new ExtraBudgetNotFoundException("nonexistent"));

        mockMvc.perform(get(BASE_PATH + "/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("ExtraBudget not found"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    // -----------------------------------------------------------------------
    // GET by walletId / bulletId
    // -----------------------------------------------------------------------

    @Test
    void GET_byQuery_walletId_returns200List() throws Exception {
        ExtraBudgetOutput o1 = sampleOutput("eb-1");
        ExtraBudgetOutput o2 = sampleOutput("eb-2");
        ExtraBudgetResponseDto r1 = sampleResponse("eb-1");
        ExtraBudgetResponseDto r2 = sampleResponse("eb-2");

        when(findExtraBudgetsByWalletIdBoundary.execute(eq("wallet-1"), anyString()))
                .thenReturn(List.of(o1, o2));
        when(mapper.toResponse(o1)).thenReturn(r1);
        when(mapper.toResponse(o2)).thenReturn(r2);

        mockMvc.perform(get(BASE_PATH + "/wallet/wallet-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("eb-1"))
                .andExpect(jsonPath("$[1].id").value("eb-2"));
    }

    @Test
    void GET_byQuery_bulletId_returns200List() throws Exception {
        ExtraBudgetOutput o1 = sampleOutput("eb-1");
        ExtraBudgetResponseDto r1 = sampleResponse("eb-1");

        when(findExtraBudgetsByBulletIdBoundary.execute(eq("bullet-1"), anyString()))
                .thenReturn(List.of(o1));
        when(mapper.toResponse(o1)).thenReturn(r1);

        mockMvc.perform(get(BASE_PATH + "/bullet/bullet-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value("eb-1"));
    }

    // -----------------------------------------------------------------------
    // DELETE
    // -----------------------------------------------------------------------

    @Test
    void DELETE_existing_returns204() throws Exception {
        mockMvc.perform(delete(BASE_PATH + "/eb-42"))
                .andExpect(status().isNoContent());

        verify(deleteExtraBudgetByIdBoundary).execute("eb-42", "legacy");
    }

    @Test
    void DELETE_notFound_returns404() throws Exception {
        doThrow(new ExtraBudgetNotFoundException("nonexistent"))
                .when(deleteExtraBudgetByIdBoundary).execute(eq("nonexistent"), anyString());

        mockMvc.perform(delete(BASE_PATH + "/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("ExtraBudget not found"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }
}
