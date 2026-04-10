package br.com.casellisoftware.budgetmanager.rest.expense;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.SaveExpenseBoundary;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseRequestDto;
import br.com.casellisoftware.budgetmanager.rest.expense.mappers.ExpenseRestMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} for {@link ExpenseController}. Validates HTTP contract:
 * status codes, headers, JSON body, validation, and error handling via
 * {@link GlobalExceptionHandler}.
 */
@WebMvcTest(controllers = ExpenseController.class)
@Import(GlobalExceptionHandler.class)
class ExpenseControllerTest {

    private static final String EXPENSES_PATH = "/expenses";
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SaveExpenseBoundary saveExpenseBoundary;

    @MockitoBean
    private ExpenseRestMapper mapper;

    // ---------- Happy path ----------

    @Test
    void save_validPayload_returns201WithLocationAndBody() throws Exception {
        var request = new ExpenseRequestDto("Groceries", new BigDecimal("59.90"), LocalDate.of(2026, 4, 10), "wallet-1");
        var input = new ExpenseInput("Groceries", new BigDecimal("59.90"), LocalDate.of(2026, 4, 10), "wallet-1");
        var output = new ExpenseOutput("exp-42", "Groceries", new BigDecimal("59.90"), LocalDate.of(2026, 4, 10), "wallet-1", new BigDecimal("940.10"));
        var responseDto = new br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto(
                "exp-42", "Groceries", new BigDecimal("59.90"), LocalDate.of(2026, 4, 10), new BigDecimal("940.10"), "wallet-1"
        );

        when(mapper.expenseRequestDtoToExpenseInput(any(ExpenseRequestDto.class))).thenReturn(input);
        when(saveExpenseBoundary.execute(input)).thenReturn(output);
        when(mapper.expenseOutputToExpenseResponseDto(output)).thenReturn(responseDto);

        mockMvc.perform(post(EXPENSES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/expenses/exp-42"))
                .andExpect(jsonPath("$.id").value("exp-42"))
                .andExpect(jsonPath("$.name").value("Groceries"))
                .andExpect(jsonPath("$.cost").value(59.90))
                .andExpect(jsonPath("$.purchaseDate").value("2026-04-10"))
                .andExpect(jsonPath("$.remaining").value(940.10))
                .andExpect(jsonPath("$.walletId").value("wallet-1"));

        ArgumentCaptor<ExpenseInput> captor = ArgumentCaptor.forClass(ExpenseInput.class);
        verify(saveExpenseBoundary, times(1)).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(input);
    }

    // ---------- Validation: single field ----------

    @Test
    void save_missingName_returns400WithFieldError() throws Exception {
        String body = """
                {
                    "cost": 10.00,
                    "purchaseDate": "2026-04-10",
                    "walletId": "wallet-1"
                }
                """;

        mockMvc.perform(post(EXPENSES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[?(@.field == 'name')]").exists())
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveExpenseBoundary, never()).execute(any());
    }

    @Test
    void save_negativeCost_returns400WithFieldError() throws Exception {
        String body = """
                {
                    "name": "Coffee",
                    "cost": -5.00,
                    "purchaseDate": "2026-04-10",
                    "walletId": "wallet-1"
                }
                """;

        mockMvc.perform(post(EXPENSES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'cost')]").exists())
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveExpenseBoundary, never()).execute(any());
    }

    // ---------- Validation: multiple errors ----------

    @Test
    void save_multipleInvalidFields_returns400WithAllFieldErrors() throws Exception {
        String body = """
                {
                    "name": "",
                    "cost": -1,
                    "walletId": ""
                }
                """;

        mockMvc.perform(post(EXPENSES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("name", "cost", "purchaseDate", "walletId")))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveExpenseBoundary, never()).execute(any());
    }

    // ---------- Malformed JSON ----------

    @Test
    void save_malformedJson_returns400() throws Exception {
        mockMvc.perform(post(EXPENSES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{broken-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Malformed request"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveExpenseBoundary, never()).execute(any());
    }

    // ---------- Type mismatch ----------

    @Test
    void save_costAsString_returns400() throws Exception {
        String body = """
                {
                    "name": "Coffee",
                    "cost": "abc",
                    "purchaseDate": "2026-04-10",
                    "walletId": "wallet-1"
                }
                """;

        mockMvc.perform(post(EXPENSES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveExpenseBoundary, never()).execute(any());
    }

    // ---------- Use case throws unexpected exception ----------

    @Test
    void save_useCaseThrowsRuntimeException_returns500WithoutStacktrace() throws Exception {
        var request = new ExpenseRequestDto("Groceries", new BigDecimal("10.00"), LocalDate.of(2026, 4, 10), "wallet-1");
        var input = new ExpenseInput("Groceries", new BigDecimal("10.00"), LocalDate.of(2026, 4, 10), "wallet-1");

        when(mapper.expenseRequestDtoToExpenseInput(any(ExpenseRequestDto.class))).thenReturn(input);
        when(saveExpenseBoundary.execute(input)).thenThrow(new RuntimeException("secret internal error"));

        mockMvc.perform(post(EXPENSES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)))
                .andExpect(content().string(not(containsString("secret internal error"))));
    }

    // ---------- Validation: future purchase date ----------

    @Test
    void save_futurePurchaseDate_returns400WithFieldError() throws Exception {
        String body = """
                {
                    "name": "Coffee",
                    "cost": 5.00,
                    "purchaseDate": "2099-01-01",
                    "walletId": "wallet-1"
                }
                """;

        mockMvc.perform(post(EXPENSES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'purchaseDate')]").exists())
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveExpenseBoundary, never()).execute(any());
    }
}
