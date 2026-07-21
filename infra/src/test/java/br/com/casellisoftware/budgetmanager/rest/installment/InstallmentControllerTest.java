package br.com.casellisoftware.budgetmanager.rest.installment;

import br.com.casellisoftware.budgetmanager.application.installment.boundary.DeleteInstallmentBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.FindInstallmentByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.FindInstallmentsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.InstallmentOutput;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.PatchInstallmentBoundary;
import br.com.casellisoftware.budgetmanager.application.installment.boundary.SaveStandaloneInstallmentBoundary;
import br.com.casellisoftware.budgetmanager.domain.creditcard.CreditCardNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentAlreadyDeletedException;
import br.com.casellisoftware.budgetmanager.domain.installment.InstallmentNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import br.com.casellisoftware.budgetmanager.rest.installment.dtos.InstallmentResponseDto;
import br.com.casellisoftware.budgetmanager.rest.installment.dtos.PagedInstallmentResponseDto;
import br.com.casellisoftware.budgetmanager.rest.installment.mappers.InstallmentRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(controllers = InstallmentController.class)
@Import(GlobalExceptionHandler.class)
class InstallmentControllerTest {

    private static final String INSTALLMENTS_PATH = "/installments";
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FindInstallmentByIdBoundary findInstallmentByIdBoundary;

    @MockitoBean
    private FindInstallmentsByWalletIdBoundary findInstallmentsByWalletIdBoundary;

    @MockitoBean
    private DeleteInstallmentBoundary deleteInstallmentBoundary;

    @MockitoBean
    private SaveStandaloneInstallmentBoundary saveStandaloneInstallmentBoundary;

    @MockitoBean
    private PatchInstallmentBoundary patchInstallmentBoundary;

    @MockitoBean
    private InstallmentRestMapper mapper;

    @Test
    void findById_whenFound_returns200WithBody() throws Exception {
        InstallmentOutput output = output("inst-1", false, null);
        InstallmentResponseDto response = response("inst-1", false, null);

        when(findInstallmentByIdBoundary.findById("inst-1", "legacy")).thenReturn(output);
        when(mapper.installmentOutputToInstallmentResponseDto(output)).thenReturn(response);

        mockMvc.perform(get(INSTALLMENTS_PATH + "/inst-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("inst-1"))
                .andExpect(jsonPath("$.installmentNumber").value(6))
                .andExpect(jsonPath("$.sourceWalletId").value("wallet-1"))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void findById_whenMissing_returns404() throws Exception {
        when(findInstallmentByIdBoundary.findById("missing", "legacy"))
                .thenThrow(new InstallmentNotFoundException("missing"));

        mockMvc.perform(get(INSTALLMENTS_PATH + "/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Installment not found"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void findByWalletId_returnsMappedList() throws Exception {
        InstallmentOutput first = output("inst-1", false, null);
        InstallmentOutput second = output("inst-2", true, LocalDateTime.of(2026, 7, 1, 12, 0));
        InstallmentResponseDto firstResponse = response("inst-1", false, null);
        InstallmentResponseDto secondResponse = response("inst-2", true, LocalDateTime.of(2026, 7, 1, 12, 0));

        PageResult<InstallmentOutput> result = new PageResult<>(List.of(first, second), 0, 20, 2, 1);
        PagedInstallmentResponseDto responsePage = new PagedInstallmentResponseDto(
                List.of(firstResponse, secondResponse), 0, 20, 2, 1);
        when(findInstallmentsByWalletIdBoundary.execute(org.mockito.ArgumentMatchers.eq("wallet-1"), any(), any()))
                .thenReturn(result);
        when(mapper.installmentOutputToInstallmentResponseDto(first)).thenReturn(firstResponse);
        when(mapper.installmentOutputToInstallmentResponseDto(second)).thenReturn(secondResponse);
        when(mapper.toPagedResponse(result)).thenReturn(responsePage);

        mockMvc.perform(get(INSTALLMENTS_PATH + "/wallet/wallet-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder("inst-1", "inst-2")));
    }

    @Test
    void findFinishedByWalletId_returnsMappedPage() throws Exception {
        InstallmentOutput first = output("fin-1", false, null);
        InstallmentResponseDto firstResponse = response("fin-1", false, null);

        PageResult<InstallmentOutput> result = new PageResult<>(List.of(first), 0, 20, 1, 1);
        PagedInstallmentResponseDto responsePage = new PagedInstallmentResponseDto(
                List.of(firstResponse), 0, 20, 1, 1);
        when(findInstallmentsByWalletIdBoundary.executeFinished(org.mockito.ArgumentMatchers.eq("wallet-1"), any(), any()))
                .thenReturn(result);
        when(mapper.toPagedResponse(result)).thenReturn(responsePage);

        mockMvc.perform(get(INSTALLMENTS_PATH + "/wallet/wallet-1/finished"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id", containsInAnyOrder("fin-1")))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(findInstallmentsByWalletIdBoundary).executeFinished(org.mockito.ArgumentMatchers.eq("wallet-1"), any(), org.mockito.ArgumentMatchers.eq("legacy"));
    }

    @Test
    void deleteById_returns204() throws Exception {
        mockMvc.perform(delete(INSTALLMENTS_PATH + "/inst-1"))
                .andExpect(status().isNoContent());

        verify(deleteInstallmentBoundary).execute("inst-1", "legacy");
    }

    @Test
    void deleteById_whenAlreadyDeleted_returns409() throws Exception {
        doThrow(new InstallmentAlreadyDeletedException("inst-1"))
                .when(deleteInstallmentBoundary).execute(org.mockito.ArgumentMatchers.eq("inst-1"), org.mockito.ArgumentMatchers.anyString());

        mockMvc.perform(delete(INSTALLMENTS_PATH + "/inst-1"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Installment already deleted"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void findByWalletId_whenBoundaryFails_propagates404() throws Exception {
        when(findInstallmentsByWalletIdBoundary.execute(org.mockito.ArgumentMatchers.eq("wallet-missing"), any(), any()))
                .thenThrow(new br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException("wallet-missing"));

        mockMvc.perform(get(INSTALLMENTS_PATH + "/wallet/wallet-missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Wallet not found"));

        verify(mapper, never()).installmentOutputToInstallmentResponseDto(org.mockito.ArgumentMatchers.any());
    }

    // ---------- POST /installments ----------

    @Test
    void save_withOriginalValue_returns201WithLocationAndBody() throws Exception {
        InstallmentOutput output = output("inst-new", false, null);
        InstallmentResponseDto responseDto = response("inst-new", false, null);

        when(saveStandaloneInstallmentBoundary.execute(any())).thenReturn(output);
        when(mapper.installmentOutputToInstallmentResponseDto(output)).thenReturn(responseDto);

        String body = """
                {
                    "description": "Notebook",
                    "originalValue": 6000.00,
                    "currency": "BRL",
                    "installmentNumber": 6,
                    "purchaseDate": "2025-01-10",
                    "creditCardId": "cc1",
                    "sourceEffectiveMonth": "2025-02",
                    "flag": "NONE"
                }
                """;

        mockMvc.perform(post(INSTALLMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/installments/inst-new"))
                .andExpect(jsonPath("$.id").value("inst-new"));
    }

    @Test
    void save_withInstallmentValue_returns201() throws Exception {
        InstallmentOutput output = output("inst-new2", false, null);
        InstallmentResponseDto responseDto = response("inst-new2", false, null);

        when(saveStandaloneInstallmentBoundary.execute(any())).thenReturn(output);
        when(mapper.installmentOutputToInstallmentResponseDto(output)).thenReturn(responseDto);

        String body = """
                {
                    "description": "Notebook",
                    "installmentValue": 1000.00,
                    "currency": "BRL",
                    "installmentNumber": 6,
                    "purchaseDate": "2025-01-10",
                    "creditCardId": "cc1",
                    "sourceEffectiveMonth": "2025-02",
                    "flag": "NONE"
                }
                """;

        mockMvc.perform(post(INSTALLMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    void save_missingDescription_returns400() throws Exception {
        String body = """
                {
                    "originalValue": 6000.00,
                    "currency": "BRL",
                    "installmentNumber": 6,
                    "purchaseDate": "2025-01-10",
                    "creditCardId": "cc1",
                    "sourceEffectiveMonth": "2025-02"
                }
                """;

        mockMvc.perform(post(INSTALLMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveStandaloneInstallmentBoundary, never()).execute(any());
    }

    @Test
    void save_missingCurrency_returns400() throws Exception {
        String body = """
                {
                    "description": "Notebook",
                    "originalValue": 6000.00,
                    "installmentNumber": 6,
                    "purchaseDate": "2025-01-10",
                    "creditCardId": "cc1",
                    "sourceEffectiveMonth": "2025-02"
                }
                """;

        mockMvc.perform(post(INSTALLMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveStandaloneInstallmentBoundary, never()).execute(any());
    }

    @Test
    void save_installmentNumberTooLow_returns400() throws Exception {
        String body = """
                {
                    "description": "Notebook",
                    "originalValue": 1000.00,
                    "currency": "BRL",
                    "installmentNumber": 1,
                    "purchaseDate": "2025-01-10",
                    "creditCardId": "cc1",
                    "sourceEffectiveMonth": "2025-02"
                }
                """;

        mockMvc.perform(post(INSTALLMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveStandaloneInstallmentBoundary, never()).execute(any());
    }

    @Test
    void save_nullSourceEffectiveMonth_returns400() throws Exception {
        String body = """
                {
                    "description": "Notebook",
                    "originalValue": 6000.00,
                    "currency": "BRL",
                    "installmentNumber": 6,
                    "purchaseDate": "2025-01-10",
                    "creditCardId": "cc1"
                }
                """;

        mockMvc.perform(post(INSTALLMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveStandaloneInstallmentBoundary, never()).execute(any());
    }

    @Test
    void save_creditCardNotFound_returns404() throws Exception {
        when(saveStandaloneInstallmentBoundary.execute(any()))
                .thenThrow(new CreditCardNotFoundException("cc-missing"));

        String body = """
                {
                    "description": "Notebook",
                    "originalValue": 6000.00,
                    "currency": "BRL",
                    "installmentNumber": 6,
                    "purchaseDate": "2025-01-10",
                    "creditCardId": "cc-missing",
                    "sourceEffectiveMonth": "2025-02",
                    "flag": "NONE"
                }
                """;

        mockMvc.perform(post(INSTALLMENTS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("CreditCard not found"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    private static InstallmentOutput output(String id, boolean deleted, LocalDateTime deletedAt) {
        return new InstallmentOutput(
                id,
                "Notebook",
                null,
                new BigDecimal("6000.00"),
                new BigDecimal("1000.00"),
                "BRL",
                6,
                LocalDate.of(2026, 5, 10),
                YearMonth.of(2026, 10),
                "cc1",
                "wallet-1",
                YearMonth.of(2026, 5),
                deleted,
                deletedAt,
                FlagEnum.NONE
        );
    }

    private static InstallmentResponseDto response(String id, boolean deleted, LocalDateTime deletedAt) {
        return new InstallmentResponseDto(
                id,
                "Notebook",
                null,
                new BigDecimal("6000.00"),
                new BigDecimal("1000.00"),
                "BRL",
                6,
                LocalDate.of(2026, 5, 10),
                YearMonth.of(2026, 10),
                "cc1",
                "wallet-1",
                YearMonth.of(2026, 5),
                deleted,
                deletedAt,
                FlagEnum.NONE,
                false,
                null,
                null,
                null
        );
    }
}
