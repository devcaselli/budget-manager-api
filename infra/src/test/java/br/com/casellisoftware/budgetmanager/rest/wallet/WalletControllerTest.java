package br.com.casellisoftware.budgetmanager.rest.wallet;

import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindAllWalletsBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.FindWalletByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.SaveWalletBoundary;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletInput;
import br.com.casellisoftware.budgetmanager.application.wallet.boundary.WalletOutput;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletRequestDto;
import br.com.casellisoftware.budgetmanager.rest.wallet.dtos.WalletResponseDto;
import br.com.casellisoftware.budgetmanager.rest.wallet.mappers.WalletRestMapper;
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

@WebMvcTest(controllers = WalletController.class)
@Import(GlobalExceptionHandler.class)
class WalletControllerTest {

    private static final String WALLETS_PATH = "/wallets";
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SaveWalletBoundary saveWalletBoundary;

    @MockitoBean
    private FindAllWalletsBoundary findAllWalletsBoundary;

    @MockitoBean
    private FindWalletByIdBoundary findWalletByIdBoundary;

    @MockitoBean
    private WalletRestMapper mapper;

    @Test
    void save_validPayload_returns201WithLocationAndBody() throws Exception {
        var request = new WalletRequestDto(
                "Monthly",
                new BigDecimal("3000.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                false
        );
        var input = new WalletInput(
                "Monthly",
                new BigDecimal("3000.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                false
        );
        var output = new WalletOutput(
                "wallet-42",
                "Monthly",
                new BigDecimal("3000.00"),
                new BigDecimal("3000.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                false
        );
        var responseDto = new WalletResponseDto(
                "wallet-42",
                "Monthly",
                new BigDecimal("3000.00"),
                new BigDecimal("3000.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                false
        );

        when(mapper.walletRequestDtoToWalletInput(any(WalletRequestDto.class))).thenReturn(input);
        when(saveWalletBoundary.execute(input)).thenReturn(output);
        when(mapper.walletOutputToWalletResponseDto(output)).thenReturn(responseDto);

        mockMvc.perform(post(WALLETS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/wallets/wallet-42"))
                .andExpect(jsonPath("$.id").value("wallet-42"))
                .andExpect(jsonPath("$.description").value("Monthly"))
                .andExpect(jsonPath("$.budget").value(3000.00))
                .andExpect(jsonPath("$.remaining").value(3000.00))
                .andExpect(jsonPath("$.startDate").value("2026-04-01"))
                .andExpect(jsonPath("$.closedDate").value("2026-05-01"))
                .andExpect(jsonPath("$.closed").value(false));

        ArgumentCaptor<WalletInput> captor = ArgumentCaptor.forClass(WalletInput.class);
        verify(saveWalletBoundary, times(1)).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(input);
    }

    @Test
    void save_invalidPayload_returns400WithFieldErrors() throws Exception {
        String body = """
                {
                    "budget": -1,
                    "closedDate": "2020-01-01"
                }
                """;

        mockMvc.perform(post(WALLETS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field",
                        containsInAnyOrder("budget", "startDate", "closedDate")))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveWalletBoundary, never()).execute(any());
    }

    @Test
    void save_useCaseThrowsRuntimeException_returns500WithoutStacktrace() throws Exception {
        var request = new WalletRequestDto(
                "Monthly",
                new BigDecimal("3000.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                false
        );
        var input = new WalletInput(
                "Monthly",
                new BigDecimal("3000.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                false
        );

        when(mapper.walletRequestDtoToWalletInput(any(WalletRequestDto.class))).thenReturn(input);
        when(saveWalletBoundary.execute(input)).thenThrow(new RuntimeException("secret internal error"));

        mockMvc.perform(post(WALLETS_PATH)
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
    void findById_happyPath_returns200WithBody() throws Exception {
        var output = new WalletOutput(
                "wallet-42",
                "Monthly",
                new BigDecimal("3000.00"),
                new BigDecimal("1200.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                false
        );
        var responseDto = new WalletResponseDto(
                "wallet-42",
                "Monthly",
                new BigDecimal("3000.00"),
                new BigDecimal("1200.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                false
        );

        when(findWalletByIdBoundary.findById("wallet-42")).thenReturn(output);
        when(mapper.walletOutputToWalletResponseDto(output)).thenReturn(responseDto);

        mockMvc.perform(get(WALLETS_PATH + "/wallet-42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("wallet-42"))
                .andExpect(jsonPath("$.description").value("Monthly"))
                .andExpect(jsonPath("$.budget").value(3000.00))
                .andExpect(jsonPath("$.remaining").value(1200.00))
                .andExpect(jsonPath("$.startDate").value("2026-04-01"))
                .andExpect(jsonPath("$.closedDate").value("2026-05-01"))
                .andExpect(jsonPath("$.closed").value(false));
    }

    @Test
    void findAll_happyPath_returns200WithWallets() throws Exception {
        var output = new WalletOutput(
                "wallet-42",
                "Monthly",
                new BigDecimal("3000.00"),
                new BigDecimal("1200.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                false
        );
        var responseDto = new WalletResponseDto(
                "wallet-42",
                "Monthly",
                new BigDecimal("3000.00"),
                new BigDecimal("1200.00"),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 5, 1),
                false
        );

        when(findAllWalletsBoundary.execute()).thenReturn(List.of(output));
        when(mapper.walletOutputToWalletResponseDto(output)).thenReturn(responseDto);

        mockMvc.perform(get(WALLETS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("wallet-42"))
                .andExpect(jsonPath("$[0].description").value("Monthly"))
                .andExpect(jsonPath("$[0].budget").value(3000.00))
                .andExpect(jsonPath("$[0].remaining").value(1200.00))
                .andExpect(jsonPath("$[0].startDate").value("2026-04-01"))
                .andExpect(jsonPath("$[0].closedDate").value("2026-05-01"))
                .andExpect(jsonPath("$[0].closed").value(false));
    }

    @Test
    void findById_whenMissing_returns404() throws Exception {
        when(findWalletByIdBoundary.findById("missing"))
                .thenThrow(new WalletNotFoundException("missing"));

        mockMvc.perform(get(WALLETS_PATH + "/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Wallet not found"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }
}
