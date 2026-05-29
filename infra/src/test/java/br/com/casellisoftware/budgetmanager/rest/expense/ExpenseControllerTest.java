package br.com.casellisoftware.budgetmanager.rest.expense;

import br.com.casellisoftware.budgetmanager.application.expense.boundary.DeleteExpenseByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseInput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.ExpenseOutput;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindAllMineExpensesBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindExpensesByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.FindMineExpensesBoundary;
import br.com.casellisoftware.budgetmanager.application.expense.boundary.SaveExpenseBoundary;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseRequestDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto;
import br.com.casellisoftware.budgetmanager.rest.expense.dtos.PagedExpenseResponseDto;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    private static final String CREDIT_CARD_ID = "cc-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SaveExpenseBoundary saveExpenseBoundary;

    @MockitoBean
    private FindExpensesByWalletIdBoundary findExpensesByWalletIdBoundary;

    @MockitoBean
    private FindMineExpensesBoundary findMineExpensesBoundary;

    @MockitoBean
    private FindAllMineExpensesBoundary findAllMineExpensesBoundary;

    @MockitoBean
    private DeleteExpenseByIdBoundary deleteExpenseByIdBoundary;

    @MockitoBean
    private ExpenseRestMapper mapper;

    // ---------- Happy path ----------

    @Test
    void save_validPayload_returns201WithLocationAndBody() throws Exception {
        var request = new ExpenseRequestDto("Groceries", new BigDecimal("59.90"), LocalDate.of(2026, 4, 10),
                "wallet-1", CREDIT_CARD_ID, false, null, FlagEnum.NONE);
        var input = new ExpenseInput("Groceries", new BigDecimal("59.90"), LocalDate.of(2026, 4, 10),
                "wallet-1", CREDIT_CARD_ID, false, null, FlagEnum.NONE);
        var output = new ExpenseOutput("exp-42", "Groceries", new BigDecimal("59.90"), LocalDate.of(2026, 4, 10),
                "wallet-1", CREDIT_CARD_ID, new BigDecimal("940.10"), List.of(), FlagEnum.NONE);
        var responseDto = new br.com.casellisoftware.budgetmanager.rest.expense.dtos.ExpenseResponseDto(
                "exp-42", "Groceries", new BigDecimal("59.90"), LocalDate.of(2026, 4, 10),
                new BigDecimal("940.10"), "wallet-1", CREDIT_CARD_ID, List.of(), FlagEnum.NONE
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
                .andExpect(jsonPath("$.walletId").value("wallet-1"))
                .andExpect(jsonPath("$.creditCardId").value(CREDIT_CARD_ID));

        ArgumentCaptor<ExpenseInput> captor = ArgumentCaptor.forClass(ExpenseInput.class);
        verify(saveExpenseBoundary, times(1)).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(input);
    }

    @Test
    void save_installmentTrueWithValidNumber_returns201() throws Exception {
        var request = new ExpenseRequestDto("Notebook", new BigDecimal("5000.00"), LocalDate.of(2026, 4, 10),
                "wallet-1", CREDIT_CARD_ID, true, 12, FlagEnum.NONE);
        var input = new ExpenseInput("Notebook", new BigDecimal("5000.00"), LocalDate.of(2026, 4, 10),
                "wallet-1", CREDIT_CARD_ID, true, 12, FlagEnum.NONE);
        var output = new ExpenseOutput("exp-99", "Notebook", new BigDecimal("416.67"), LocalDate.of(2026, 4, 10),
                "wallet-1", CREDIT_CARD_ID, new BigDecimal("416.67"), List.of(), FlagEnum.NONE);
        var responseDto = new ExpenseResponseDto(
                "exp-99", "Notebook", new BigDecimal("416.67"), LocalDate.of(2026, 4, 10),
                new BigDecimal("416.67"), "wallet-1", CREDIT_CARD_ID, List.of(), FlagEnum.NONE
        );

        when(mapper.expenseRequestDtoToExpenseInput(any(ExpenseRequestDto.class))).thenReturn(input);
        when(saveExpenseBoundary.execute(input)).thenReturn(output);
        when(mapper.expenseOutputToExpenseResponseDto(output)).thenReturn(responseDto);

        mockMvc.perform(post(EXPENSES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("exp-99"))
                .andExpect(jsonPath("$.creditCardId").value(CREDIT_CARD_ID));
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
                    "walletId": "wallet-1",
                    "creditCardId": "cc-1"
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
                    "walletId": "",
                    "creditCardId": ""
                }
                """;

        mockMvc.perform(post(EXPENSES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("name", "cost", "purchaseDate", "walletId", "creditCardId")))
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
                    "walletId": "wallet-1",
                    "creditCardId": "cc-1"
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
        var request = new ExpenseRequestDto("Groceries", new BigDecimal("10.00"), LocalDate.of(2026, 4, 10),
                "wallet-1", CREDIT_CARD_ID, false, null, FlagEnum.NONE);
        var input = new ExpenseInput("Groceries", new BigDecimal("10.00"), LocalDate.of(2026, 4, 10),
                "wallet-1", CREDIT_CARD_ID, false, null, FlagEnum.NONE);

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
                    "walletId": "wallet-1",
                    "creditCardId": "cc-1"
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

    @Test
    void save_missingCreditCardId_returns400WithFieldError() throws Exception {
        String body = """
                {
                    "name": "Coffee",
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
                .andExpect(jsonPath("$.errors[?(@.field == 'creditCardId')]").exists());

        verify(saveExpenseBoundary, never()).execute(any());
    }

    @Test
    void save_installmentTrueWithoutNumber_returns400() throws Exception {
        String body = """
                {
                    "name": "Notebook",
                    "cost": 5000.00,
                    "purchaseDate": "2026-04-10",
                    "walletId": "wallet-1",
                    "creditCardId": "cc-1",
                    "installment": true
                }
                """;

        mockMvc.perform(post(EXPENSES_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));

        verify(saveExpenseBoundary, never()).execute(any());
    }

    // ---------- Find mine ----------

    @Test
    void findMine_defaultMonths_returnsFlatArray() throws Exception {
        var output = new ExpenseOutput(
                "exp-1",
                "Lunch",
                new BigDecimal("10.50"),
                LocalDate.of(2026, 4, 10),
                new BigDecimal("10.50"),
                "wallet-1",
                null,
                CREDIT_CARD_ID,
                false,
                null,
                List.of(),
                FlagEnum.NONE
        );
        var responseDto = new ExpenseResponseDto(
                "exp-1",
                "Lunch",
                new BigDecimal("10.50"),
                LocalDate.of(2026, 4, 10),
                new BigDecimal("10.50"),
                "wallet-1",
                null,
                CREDIT_CARD_ID,
                false,
                null,
                null
        );

        when(findMineExpensesBoundary.execute(12, "legacy")).thenReturn(List.of(output));
        when(mapper.expenseOutputToExpenseResponseDto(output)).thenReturn(responseDto);

        mockMvc.perform(get(EXPENSES_PATH + "/mine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("exp-1"))
                .andExpect(jsonPath("$[0].name").value("Lunch"))
                .andExpect(jsonPath("$[0].cost").value(10.50))
                .andExpect(jsonPath("$[0].purchaseDate").value("2026-04-10"))
                .andExpect(jsonPath("$[0].remaining").value(10.50))
                .andExpect(jsonPath("$[0].walletId").value("wallet-1"))
                .andExpect(jsonPath("$[0].bulletId").doesNotExist())
                .andExpect(jsonPath("$[0].creditCardId").value(CREDIT_CARD_ID))
                .andExpect(jsonPath("$[0].installment").value(false))
                .andExpect(jsonPath("$[0].installmentNumber").doesNotExist());

        verify(findMineExpensesBoundary).execute(12, "legacy");
    }

    @Test
    void findMine_months24_passesQueryParamThrough() throws Exception {
        when(findMineExpensesBoundary.execute(24, "legacy")).thenReturn(List.of());

        mockMvc.perform(get(EXPENSES_PATH + "/mine")
                        .param("months", "24"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));

        verify(findMineExpensesBoundary).execute(24, "legacy");
    }

    @Test
    void findMine_invalidMonths_returns400() throws Exception {
        mockMvc.perform(get(EXPENSES_PATH + "/mine")
                        .param("months", "6"))
                .andExpect(status().isBadRequest());

        verify(findMineExpensesBoundary, never()).execute(anyInt(), anyString());
    }

    @Test
    void exportMine_returnsCsvAttachment() throws Exception {
        var output = new ExpenseOutput(
                "exp-1",
                "Lunch, office",
                new BigDecimal("10.50"),
                LocalDate.of(2026, 4, 10),
                new BigDecimal("0.00"),
                "wallet-1",
                null,
                CREDIT_CARD_ID,
                true,
                12,
                List.of(),
                FlagEnum.NONE
        );
        var responseDto = new ExpenseResponseDto(
                "exp-1",
                "Lunch, office",
                new BigDecimal("10.50"),
                LocalDate.of(2026, 4, 10),
                new BigDecimal("0.00"),
                "wallet-1",
                null,
                CREDIT_CARD_ID,
                true,
                12,
                null
        );

        when(findAllMineExpensesBoundary.execute("legacy")).thenReturn(List.of(output));
        when(mapper.expenseOutputToExpenseResponseDto(output)).thenReturn(responseDto);

        mockMvc.perform(get(EXPENSES_PATH + "/mine/export"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("expenses.csv")))
                .andExpect(content().string("""
                        id,name,cost,purchaseDate,remaining,walletId,bulletId,creditCardId,installment,installmentNumber
                        exp-1,"Lunch, office",10.50,2026-04-10,0.00,wallet-1,,cc-1,true,12
                        """));
    }

    // ---------- Find by wallet id ----------

    @Test
    void findByWalletId_happyPath_returns200WithPagedBody() throws Exception {
        var output1 = new ExpenseOutput("exp-1", "Lunch", new BigDecimal("10.50"),
                LocalDate.of(2026, 4, 10), "wallet-1", CREDIT_CARD_ID, new BigDecimal("10.50"), List.of(), FlagEnum.NONE);
        var output2 = new ExpenseOutput("exp-2", "Coffee", new BigDecimal("5.00"),
                LocalDate.of(2026, 4, 11), "wallet-1", CREDIT_CARD_ID, new BigDecimal("5.00"), List.of(), FlagEnum.NONE);

        PageResult<ExpenseOutput> pageResult = new PageResult<>(
                List.of(output1, output2), 0, 20, 2, 1
        );

        var responseDto1 = new ExpenseResponseDto("exp-1", "Lunch", new BigDecimal("10.50"),
                LocalDate.of(2026, 4, 10), new BigDecimal("10.50"), "wallet-1", CREDIT_CARD_ID, List.of(), FlagEnum.NONE);
        var responseDto2 = new ExpenseResponseDto("exp-2", "Coffee", new BigDecimal("5.00"),
                LocalDate.of(2026, 4, 11), new BigDecimal("5.00"), "wallet-1", CREDIT_CARD_ID, List.of(), FlagEnum.NONE);

        var pagedResponse = new PagedExpenseResponseDto(
                List.of(responseDto1, responseDto2), 0, 20, 2, 1
        );

        when(findExpensesByWalletIdBoundary.execute(eq("wallet-1"), eq(0), eq(20), eq(false), anyString())).thenReturn(pageResult);
        when(mapper.toPagedResponse(pageResult)).thenReturn(pagedResponse);

        mockMvc.perform(get(EXPENSES_PATH + "/wallet/wallet-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value("exp-1"))
                .andExpect(jsonPath("$.content[0].name").value("Lunch"))
                .andExpect(jsonPath("$.content[1].id").value("exp-2"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void findByWalletId_withCustomPageParams_passesThemThrough() throws Exception {
        PageResult<ExpenseOutput> pageResult = new PageResult<>(List.of(), 2, 5, 0, 0);
        var pagedResponse = new PagedExpenseResponseDto(List.of(), 2, 5, 0, 0);

        when(findExpensesByWalletIdBoundary.execute(eq("wallet-1"), eq(2), eq(5), eq(false), anyString())).thenReturn(pageResult);
        when(mapper.toPagedResponse(pageResult)).thenReturn(pagedResponse);

        mockMvc.perform(get(EXPENSES_PATH + "/wallet/wallet-1")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void findByWalletId_walletNotFound_returns404() throws Exception {
        when(findExpensesByWalletIdBoundary.execute(eq("nonexistent"), anyInt(), anyInt(), eq(false), anyString()))
                .thenThrow(new WalletNotFoundException("nonexistent"));

        mockMvc.perform(get(EXPENSES_PATH + "/wallet/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Wallet not found"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void findByWalletId_negativePage_returns400() throws Exception {
        mockMvc.perform(get(EXPENSES_PATH + "/wallet/wallet-1")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verify(findExpensesByWalletIdBoundary, never()).execute(any(), anyInt(), anyInt(), anyBoolean(), anyString());
    }

    @Test
    void findByWalletId_sizeExceedsMax_returns400() throws Exception {
        mockMvc.perform(get(EXPENSES_PATH + "/wallet/wallet-1")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        verify(findExpensesByWalletIdBoundary, never()).execute(any(), anyInt(), anyInt(), anyBoolean(), anyString());
    }

    @Test
    void findByWalletId_unhiddenTrue_passesFilterThrough() throws Exception {
        PageResult<ExpenseOutput> pageResult = new PageResult<>(List.of(), 0, 20, 0, 0);
        var pagedResponse = new PagedExpenseResponseDto(List.of(), 0, 20, 0, 0);

        when(findExpensesByWalletIdBoundary.execute(eq("wallet-1"), eq(0), eq(20), eq(true), anyString())).thenReturn(pageResult);
        when(mapper.toPagedResponse(pageResult)).thenReturn(pagedResponse);

        mockMvc.perform(get(EXPENSES_PATH + "/wallet/wallet-1")
                        .param("unhidden", "true"))
                .andExpect(status().isOk());

        verify(findExpensesByWalletIdBoundary).execute(eq("wallet-1"), eq(0), eq(20), eq(true), anyString());
    }
}
