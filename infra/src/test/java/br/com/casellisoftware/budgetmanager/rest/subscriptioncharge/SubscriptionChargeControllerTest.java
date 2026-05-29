package br.com.casellisoftware.budgetmanager.rest.subscriptioncharge;

import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.FindSubscriptionChargesByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.subscriptioncharge.boundary.SubscriptionChargeOutput;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import br.com.casellisoftware.budgetmanager.rest.subscriptioncharge.dtos.SubscriptionChargeResponseDto;
import br.com.casellisoftware.budgetmanager.rest.subscriptioncharge.mappers.SubscriptionChargeRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SubscriptionChargeController.class)
@Import(GlobalExceptionHandler.class)
class SubscriptionChargeControllerTest {

    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindSubscriptionChargesByWalletIdBoundary findSubscriptionChargesByWalletIdBoundary;

    @MockitoBean
    private SubscriptionChargeRestMapper mapper;

    @Test
    void findByWalletId_whenFound_returns200WithCharges() throws Exception {
        SubscriptionChargeOutput output = new SubscriptionChargeOutput(
                "charge-1",
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                new BigDecimal("55.90"),
                new BigDecimal("20.00")
        );
        SubscriptionChargeResponseDto response = new SubscriptionChargeResponseDto(
                "charge-1",
                "subscription-1",
                "wallet-1",
                YearMonth.of(2026, 5),
                new BigDecimal("55.90"),
                new BigDecimal("20.00")
        );

        when(findSubscriptionChargesByWalletIdBoundary.execute("wallet-1", "legacy")).thenReturn(List.of(output));
        when(mapper.toResponse(output)).thenReturn(response);

        mockMvc.perform(get("/wallets/wallet-1/subscription-charges"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value("charge-1"))
                .andExpect(jsonPath("$[0].subscriptionId").value("subscription-1"))
                .andExpect(jsonPath("$[0].month").value("2026-05"))
                .andExpect(jsonPath("$[0].amount").value(55.90))
                .andExpect(jsonPath("$[0].remaining").value(20.00));
    }

    @Test
    void findByWalletId_whenWalletMissing_returns404() throws Exception {
        when(findSubscriptionChargesByWalletIdBoundary.execute("missing", "legacy"))
                .thenThrow(new WalletNotFoundException("missing"));

        mockMvc.perform(get("/wallets/missing/subscription-charges"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Wallet not found"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }
}
