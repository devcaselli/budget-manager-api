package br.com.casellisoftware.budgetmanager.rest.bullet;

import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletInput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.BulletOutput;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.DeleteBulletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.FindBulletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletBoundary;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.PatchBulletInput;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletInUseException;
import br.com.casellisoftware.budgetmanager.application.bullet.boundary.SaveBulletBoundary;
import br.com.casellisoftware.budgetmanager.domain.bullet.BulletNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletAllocationExceededException;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletCurrencyMismatchException;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletRequestDto;
import br.com.casellisoftware.budgetmanager.rest.bullet.dtos.BulletResponseDto;
import br.com.casellisoftware.budgetmanager.rest.bullet.mappers.BulletRestMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} for {@link BulletController}. Validates HTTP contract:
 * status codes, headers, JSON body, validation, and error handling via
 * {@link GlobalExceptionHandler}.
 */
@WebMvcTest(controllers = BulletController.class)
@Import(GlobalExceptionHandler.class)
class BulletControllerTest {

    private static final String BULLETS_PATH = "/bullets";
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SaveBulletBoundary saveBulletBoundary;

    @MockitoBean
    private FindBulletByIdBoundary findBulletByIdBoundary;

    @MockitoBean
    private PatchBulletBoundary patchBulletBoundary;

    @MockitoBean
    private DeleteBulletByIdBoundary deleteBulletByIdBoundary;

    @MockitoBean
    private BulletRestMapper mapper;

    // ---------- Save: Happy path ----------

    @Test
    void save_validPayload_returns201WithLocationAndBody() throws Exception {
        var request = new BulletRequestDto("Rent", new BigDecimal("1500.00"), "wallet-1");
        var input = new BulletInput("Rent", new BigDecimal("1500.00"), "wallet-1");
        var output = new BulletOutput("bullet-42", "Rent", new BigDecimal("1500.00"),
                new BigDecimal("1500.00"), "wallet-1");
        var responseDto = new BulletResponseDto("bullet-42", "Rent", new BigDecimal("1500.00"),
                new BigDecimal("1500.00"), "wallet-1");

        when(mapper.bulletRequestDtoToBulletInput(any(BulletRequestDto.class))).thenReturn(input);
        when(saveBulletBoundary.execute(input)).thenReturn(output);
        when(mapper.bulletOutputToBulletResponseDto(output)).thenReturn(responseDto);

        mockMvc.perform(post(BULLETS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/bullets/bullet-42"))
                .andExpect(jsonPath("$.id").value("bullet-42"))
                .andExpect(jsonPath("$.description").value("Rent"))
                .andExpect(jsonPath("$.budget").value(1500.00))
                .andExpect(jsonPath("$.remaining").value(1500.00))
                .andExpect(jsonPath("$.walletId").value("wallet-1"));

        ArgumentCaptor<BulletInput> captor = ArgumentCaptor.forClass(BulletInput.class);
        verify(saveBulletBoundary, times(1)).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(input);
    }

    // ---------- Save: Validation ----------

    @Test
    void save_missingDescription_returns400WithFieldError() throws Exception {
        String body = """
                {
                    "budget": 100.00,
                    "walletId": "wallet-1"
                }
                """;

        mockMvc.perform(post(BULLETS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[?(@.field == 'description')]").exists())
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveBulletBoundary, never()).execute(any());
    }

    @Test
    void save_negativeBudget_returns400WithFieldError() throws Exception {
        String body = """
                {
                    "description": "Rent",
                    "budget": -100.00,
                    "walletId": "wallet-1"
                }
                """;

        mockMvc.perform(post(BULLETS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'budget')]").exists())
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveBulletBoundary, never()).execute(any());
    }

    @Test
    void save_multipleInvalidFields_returns400WithAllFieldErrors() throws Exception {
        String body = """
                {
                    "description": "",
                    "budget": -1,
                    "walletId": ""
                }
                """;

        mockMvc.perform(post(BULLETS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("description", "budget", "walletId")))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveBulletBoundary, never()).execute(any());
    }

    // ---------- Save: Malformed JSON ----------

    @Test
    void save_malformedJson_returns400() throws Exception {
        mockMvc.perform(post(BULLETS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{broken-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Malformed request"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveBulletBoundary, never()).execute(any());
    }

    // ---------- Save: Use case throws unexpected exception ----------

    @Test
    void save_useCaseThrowsRuntimeException_returns500WithoutStacktrace() throws Exception {
        var request = new BulletRequestDto("Rent", new BigDecimal("1500.00"), "wallet-1");
        var input = new BulletInput("Rent", new BigDecimal("1500.00"), "wallet-1");

        when(mapper.bulletRequestDtoToBulletInput(any(BulletRequestDto.class))).thenReturn(input);
        when(saveBulletBoundary.execute(input)).thenThrow(new RuntimeException("secret internal error"));

        mockMvc.perform(post(BULLETS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)))
                .andExpect(content().string(not(containsString("secret internal error"))));
    }

    @Test
    void save_whenBudgetExceedsWalletRemaining_returns409() throws Exception {
        var request = new BulletRequestDto("Rent", new BigDecimal("1500.00"), "wallet-1");
        var input = new BulletInput("Rent", new BigDecimal("1500.00"), "wallet-1");

        when(mapper.bulletRequestDtoToBulletInput(any(BulletRequestDto.class))).thenReturn(input);
        when(saveBulletBoundary.execute(input))
                .thenThrow(new WalletAllocationExceededException("exceeds wallet remaining"));

        mockMvc.perform(post(BULLETS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Domain conflict"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void save_whenCurrencyDiffers_returns422() throws Exception {
        var request = new BulletRequestDto("Rent", new BigDecimal("1500.00"), "wallet-1");
        var input = new BulletInput("Rent", new BigDecimal("1500.00"), "wallet-1");

        when(mapper.bulletRequestDtoToBulletInput(any(BulletRequestDto.class))).thenReturn(input);
        when(saveBulletBoundary.execute(input))
                .thenThrow(new WalletCurrencyMismatchException("Currency mismatch"));

        mockMvc.perform(post(BULLETS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Domain rule violation"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    // ---------- FindById: Happy path ----------

    @Test
    void findById_happyPath_returns200WithBody() throws Exception {
        var output = new BulletOutput("bullet-42", "Rent", new BigDecimal("1500.00"),
                new BigDecimal("1500.00"), "wallet-1");
        var responseDto = new BulletResponseDto("bullet-42", "Rent", new BigDecimal("1500.00"),
                new BigDecimal("1500.00"), "wallet-1");

        when(findBulletByIdBoundary.execute("bullet-42")).thenReturn(output);
        when(mapper.bulletOutputToBulletResponseDto(output)).thenReturn(responseDto);

        mockMvc.perform(get(BULLETS_PATH + "/bullet-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("bullet-42"))
                .andExpect(jsonPath("$.description").value("Rent"))
                .andExpect(jsonPath("$.budget").value(1500.00))
                .andExpect(jsonPath("$.remaining").value(1500.00))
                .andExpect(jsonPath("$.walletId").value("wallet-1"));
    }

    // ---------- FindById: Not found ----------

    @Test
    void findById_notFound_returns404() throws Exception {
        when(findBulletByIdBoundary.execute("nonexistent"))
                .thenThrow(new BulletNotFoundException("nonexistent"));

        mockMvc.perform(get(BULLETS_PATH + "/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Bullet not found"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    // ---------- FindById: Use case throws unexpected exception ----------

    @Test
    void findById_useCaseThrowsRuntimeException_returns500WithoutStacktrace() throws Exception {
        when(findBulletByIdBoundary.execute("bullet-1"))
                .thenThrow(new RuntimeException("secret internal error"));

        mockMvc.perform(get(BULLETS_PATH + "/bullet-1"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)))
                .andExpect(content().string(not(containsString("secret internal error"))));
    }

    @Test
    void patch_happyPath_returns200WithBody() throws Exception {
        var request = new BulletPatchRequestDto("Groceries", new BigDecimal("650.00"), null, "wallet-1");
        var input = new PatchBulletInput("bullet-42", "Groceries", new BigDecimal("650.00"), null, "wallet-1");
        var output = new BulletOutput("bullet-42", "Groceries", new BigDecimal("650.00"),
                new BigDecimal("650.00"), "wallet-1");
        var responseDto = new BulletResponseDto("bullet-42", "Groceries", new BigDecimal("650.00"),
                new BigDecimal("650.00"), "wallet-1");

        when(mapper.bulletPatchRequestDtoToInput("bullet-42", request)).thenReturn(input);
        when(patchBulletBoundary.execute(input)).thenReturn(output);
        when(mapper.bulletOutputToBulletResponseDto(output)).thenReturn(responseDto);

        mockMvc.perform(patch(BULLETS_PATH + "/bullet-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("bullet-42"))
                .andExpect(jsonPath("$.description").value("Groceries"))
                .andExpect(jsonPath("$.budget").value(650.00));
    }

    @Test
    void patch_whenBudgetReductionCutsConsumedAmount_returns409() throws Exception {
        var request = new BulletPatchRequestDto(null, new BigDecimal("100.00"), null, null);
        var input = new PatchBulletInput("bullet-42", null, new BigDecimal("100.00"), null, null);

        when(mapper.bulletPatchRequestDtoToInput("bullet-42", request)).thenReturn(input);
        when(patchBulletBoundary.execute(input))
                .thenThrow(new WalletAllocationExceededException("already consumed"));

        mockMvc.perform(patch(BULLETS_PATH + "/bullet-42")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Domain conflict"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void delete_happyPath_returns204() throws Exception {
        mockMvc.perform(delete(BULLETS_PATH + "/bullet-42"))
                .andExpect(status().isNoContent());

        verify(deleteBulletByIdBoundary).execute("bullet-42");
    }

    @Test
    void delete_whenBulletHasPayments_returns409() throws Exception {
        org.mockito.Mockito.doThrow(new BulletInUseException("bullet-42"))
                .when(deleteBulletByIdBoundary).execute("bullet-42");

        mockMvc.perform(delete(BULLETS_PATH + "/bullet-42"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Domain conflict"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }
}
