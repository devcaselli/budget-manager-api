package br.com.casellisoftware.budgetmanager.rest.creditcard;

import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardExpensesOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardInput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.CreditCardOutput;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.DeleteCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardExpensesBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindAllCreditCardsBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.creditcard.boundary.SaveCreditCardBoundary;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardInUseException;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardExpensesResponseDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardRequestDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.CreditCardResponseDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.dtos.PagedCreditCardResponseDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import br.com.casellisoftware.budgetmanager.rest.creditcard.mappers.CreditCardRestMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
 * {@code @WebMvcTest} for {@link CreditCardController}. Verifies the HTTP
 * contract for the CreditCard endpoints — status codes, Location header,
 * validation errors, paging parameters, and propagation of domain exceptions
 * through {@link GlobalExceptionHandler}.
 */
@WebMvcTest(controllers = CreditCardController.class)
@Import(GlobalExceptionHandler.class)
class CreditCardControllerTest {

    private static final String CREDIT_CARDS_PATH = "/credit-cards";
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SaveCreditCardBoundary saveCreditCardBoundary;

    @MockitoBean
    private FindCreditCardByIdBoundary findCreditCardByIdBoundary;

    @MockitoBean
    private FindAllCreditCardsBoundary findAllCreditCardsBoundary;

    @MockitoBean
    private FindCreditCardExpensesBoundary findCreditCardExpensesBoundary;

    @MockitoBean
    private br.com.casellisoftware.budgetmanager.application.creditcard.boundary.FindCreditCardChargesBoundary findCreditCardChargesBoundary;

    @MockitoBean
    private DeleteCreditCardByIdBoundary deleteCreditCardByIdBoundary;

    @MockitoBean
    private br.com.casellisoftware.budgetmanager.application.creditcard.boundary.PatchCreditCardBoundary patchCreditCardBoundary;

    @MockitoBean
    private CreditCardRestMapper mapper;

    // ---------- POST ----------

    @Test
    void save_validPayload_returns201WithLocationAndBody() throws Exception {
        var request = new CreditCardRequestDto("Nubank");
        var input = new CreditCardInput("Nubank");
        var output = new CreditCardOutput("cc-42", "Nubank", java.util.List.of());
        var responseDto = new CreditCardResponseDto("cc-42", "Nubank", java.util.List.of());

        when(mapper.creditCardRequestDtoToCreditCardInput(any(CreditCardRequestDto.class))).thenReturn(input);
        when(saveCreditCardBoundary.execute(input)).thenReturn(output);
        when(mapper.creditCardOutputToCreditCardResponseDto(output)).thenReturn(responseDto);

        mockMvc.perform(post(CREDIT_CARDS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/credit-cards/cc-42"))
                .andExpect(jsonPath("$.id").value("cc-42"))
                .andExpect(jsonPath("$.name").value("Nubank"));

        ArgumentCaptor<CreditCardInput> captor = ArgumentCaptor.forClass(CreditCardInput.class);
        verify(saveCreditCardBoundary, times(1)).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(input);
    }

    @Test
    void save_blankName_returns400WithFieldError() throws Exception {
        String body = """
                {
                    "name": "  "
                }
                """;

        mockMvc.perform(post(CREDIT_CARDS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveCreditCardBoundary, never()).execute(any());
    }

    @Test
    void save_missingName_returns400WithFieldError() throws Exception {
        String body = "{}";

        mockMvc.perform(post(CREDIT_CARDS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveCreditCardBoundary, never()).execute(any());
    }

    @Test
    void save_nameTooLong_returns400WithFieldError() throws Exception {
        String tooLongName = "x".repeat(81);
        String body = objectMapper.writeValueAsString(new CreditCardRequestDto(tooLongName));

        mockMvc.perform(post(CREDIT_CARDS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists());

        verify(saveCreditCardBoundary, never()).execute(any());
    }

    @Test
    void save_malformedJson_returns400() throws Exception {
        mockMvc.perform(post(CREDIT_CARDS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{broken-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Malformed request"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveCreditCardBoundary, never()).execute(any());
    }

    // ---------- GET /{id} ----------

    @Test
    void findById_happyPath_returns200WithBody() throws Exception {
        var output = new CreditCardOutput("cc-42", "Nubank", java.util.List.of());
        var responseDto = new CreditCardResponseDto("cc-42", "Nubank", java.util.List.of());

        when(findCreditCardByIdBoundary.findById(eq("cc-42"), anyString())).thenReturn(output);
        when(mapper.creditCardOutputToCreditCardResponseDto(output)).thenReturn(responseDto);

        mockMvc.perform(get(CREDIT_CARDS_PATH + "/cc-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("cc-42"))
                .andExpect(jsonPath("$.name").value("Nubank"));
    }

    @Test
    void findById_missing_returns404FromGlobalHandler() throws Exception {
        when(findCreditCardByIdBoundary.findById(eq("missing"), anyString()))
                .thenThrow(new CreditCardNotFoundException("missing"));

        mockMvc.perform(get(CREDIT_CARDS_PATH + "/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("CreditCard not found"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    // ---------- GET (paged) ----------

    @Test
    void findAll_happyPath_returns200WithPagedBody() throws Exception {
        var output1 = new CreditCardOutput("cc-1", "Nubank", java.util.List.of());
        var output2 = new CreditCardOutput("cc-2", "Itaú", java.util.List.of());

        PageResult<CreditCardOutput> pageResult = new PageResult<>(
                List.of(output1, output2), 0, 20, 2, 1);

        var responseDto1 = new CreditCardResponseDto("cc-1", "Nubank", java.util.List.of());
        var responseDto2 = new CreditCardResponseDto("cc-2", "Itaú", java.util.List.of());
        var paged = new PagedCreditCardResponseDto(List.of(responseDto1, responseDto2), 0, 20, 2, 1);

        when(findAllCreditCardsBoundary.execute(eq(0), eq(20), anyString())).thenReturn(pageResult);
        when(mapper.toPagedResponse(pageResult)).thenReturn(paged);

        mockMvc.perform(get(CREDIT_CARDS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder("cc-1", "cc-2")))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void findAll_respectsPageAndSizeParams() throws Exception {
        PageResult<CreditCardOutput> pageResult = new PageResult<>(List.of(), 2, 5, 0, 0);
        var paged = new PagedCreditCardResponseDto(List.of(), 2, 5, 0, 0);

        when(findAllCreditCardsBoundary.execute(eq(2), eq(5), anyString())).thenReturn(pageResult);
        when(mapper.toPagedResponse(pageResult)).thenReturn(paged);

        mockMvc.perform(get(CREDIT_CARDS_PATH).param("page", "2").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));

        verify(findAllCreditCardsBoundary).execute(eq(2), eq(5), anyString());
    }

    @Test
    void findAll_negativePage_returns400() throws Exception {
        mockMvc.perform(get(CREDIT_CARDS_PATH).param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(findAllCreditCardsBoundary, never()).execute(anyInt(), anyInt(), anyString());
    }

    @Test
    void findAll_sizeOverLimit_returns400() throws Exception {
        mockMvc.perform(get(CREDIT_CARDS_PATH).param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(findAllCreditCardsBoundary, never()).execute(anyInt(), anyInt(), anyString());
    }

    @Test
    void findExpensesByCreditCardId_happyPath_returns200() throws Exception {
        CreditCardExpensesOutput output = new CreditCardExpensesOutput(
                new PageResult<>(List.of(), 0, 20, 1, 1),
                new BigDecimal("99.90")
        );
        CreditCardExpensesResponseDto response = new CreditCardExpensesResponseDto(
                List.of(new ExpenseResponseDto("exp-1", "Uber", new BigDecimal("99.90"), null,
                        new BigDecimal("99.90"), "wallet-1", "cc-1", List.of(), null)),
                0,
                20,
                1,
                1,
                new BigDecimal("99.90")
        );

        when(findCreditCardExpensesBoundary.execute(eq("cc-1"), any(), anyString())).thenReturn(output);
        when(mapper.toCreditCardExpensesResponseDto(output)).thenReturn(response);

        mockMvc.perform(get(CREDIT_CARDS_PATH + "/cc-1/expenses")
                        .param("effectiveMonth", "2026-05")
                        .param("name", "Uber"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("exp-1"))
                .andExpect(jsonPath("$.content[0].creditCardId").value("cc-1"))
                .andExpect(jsonPath("$.totalCost").value(99.90));
    }

    @Test
    void findExpensesByCreditCardId_invalidMonthFormat_returns400() throws Exception {
        mockMvc.perform(get(CREDIT_CARDS_PATH + "/cc-1/expenses")
                        .param("effectiveMonth", "2026/05"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"));

        verify(findCreditCardExpensesBoundary, never()).execute(any(), any(), anyString());
    }

    @Test
    void findExpensesByCreditCardId_missingCard_returns404() throws Exception {
        when(findCreditCardExpensesBoundary.execute(eq("missing"), any(), anyString()))
                .thenThrow(new CreditCardNotFoundException("missing"));

        mockMvc.perform(get(CREDIT_CARDS_PATH + "/missing/expenses"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("CreditCard not found"));
    }

    // ---------- DELETE ----------

    @Test
    void deleteById_happyPath_returns204() throws Exception {
        mockMvc.perform(delete(CREDIT_CARDS_PATH + "/cc-42"))
                .andExpect(status().isNoContent());

        verify(deleteCreditCardByIdBoundary).execute(eq("cc-42"), anyString());
    }

    @Test
    void deleteById_missing_returns404FromGlobalHandler() throws Exception {
        org.mockito.Mockito.doThrow(new CreditCardNotFoundException("missing"))
                .when(deleteCreditCardByIdBoundary).execute(eq("missing"), anyString());

        mockMvc.perform(delete(CREDIT_CARDS_PATH + "/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("CreditCard not found"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void deleteById_referenced_returns409WithReferenceCounts() throws Exception {
        var refs = new CreditCardInUseException("cc-1",
                List.of("exp-1", "exp-2"),
                List.of("inst-1"));
        org.mockito.Mockito.doThrow(refs)
                .when(deleteCreditCardByIdBoundary).execute(eq("cc-1"), anyString());

        mockMvc.perform(delete(CREDIT_CARDS_PATH + "/cc-1"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("CreditCard in use"))
                .andExpect(jsonPath("$.expenseCount").value(2))
                .andExpect(jsonPath("$.installmentCount").value(1))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }
}
