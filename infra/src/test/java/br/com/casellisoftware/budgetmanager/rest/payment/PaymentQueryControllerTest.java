package br.com.casellisoftware.budgetmanager.rest.payment;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.FindPaymentsByWalletIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.wallet.exception.WalletNotFoundException;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentQueryController.class)
@Import(GlobalExceptionHandler.class)
class PaymentQueryControllerTest {

    private static final String PAYMENTS_PATH = "/payments";
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FindPaymentsByWalletIdBoundary findPaymentsByWalletIdBoundary;

    @Test
    void findByWalletId_happyPath_returns200WithPagedBody() throws Exception {
        var output1 = new PaymentOutput("pay-1", Money.of("10.50"), Instant.parse("2026-04-10T12:00:00Z"),
                "coffee", "expense-1", "wallet-1", null);
        var output2 = new PaymentOutput("pay-2", Money.of("25.00"), Instant.parse("2026-04-11T12:00:00Z"),
                "lunch", "expense-2", "wallet-1", "bullet-1");

        PageResult<PaymentOutput> pageResult = new PageResult<>(List.of(output1, output2), 0, 20, 2, 1);

        when(findPaymentsByWalletIdBoundary.execute(eq("wallet-1"), eq(0), eq(20), any())).thenReturn(pageResult);

        mockMvc.perform(get(PAYMENTS_PATH + "/wallet/wallet-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id").value("pay-1"))
                .andExpect(jsonPath("$.content[0].amount").value(10.50))
                .andExpect(jsonPath("$.content[0].walletId").value("wallet-1"))
                .andExpect(jsonPath("$.content[1].id").value("pay-2"))
                .andExpect(jsonPath("$.content[1].bulletId").value("bullet-1"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void findByWalletId_withCustomPageParams_passesThemThrough() throws Exception {
        PageResult<PaymentOutput> pageResult = new PageResult<>(List.of(), 2, 5, 0, 0);

        when(findPaymentsByWalletIdBoundary.execute(eq("wallet-1"), eq(2), eq(5), any())).thenReturn(pageResult);

        mockMvc.perform(get(PAYMENTS_PATH + "/wallet/wallet-1")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5));
    }

    @Test
    void findByWalletId_walletNotFound_returns404() throws Exception {
        when(findPaymentsByWalletIdBoundary.execute(eq("nonexistent"), anyInt(), anyInt(), any()))
                .thenThrow(new WalletNotFoundException("nonexistent"));

        mockMvc.perform(get(PAYMENTS_PATH + "/wallet/nonexistent"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Wallet not found"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void findByWalletId_negativePage_returns400() throws Exception {
        mockMvc.perform(get(PAYMENTS_PATH + "/wallet/wallet-1")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());

        verify(findPaymentsByWalletIdBoundary, never()).execute(any(), anyInt(), anyInt(), any());
    }

    @Test
    void findByWalletId_sizeExceedsMax_returns400() throws Exception {
        mockMvc.perform(get(PAYMENTS_PATH + "/wallet/wallet-1")
                        .param("size", "101"))
                .andExpect(status().isBadRequest());

        verify(findPaymentsByWalletIdBoundary, never()).execute(any(), anyInt(), anyInt(), any());
    }

    @Test
    void findByWalletId_sizeZero_returns400() throws Exception {
        mockMvc.perform(get(PAYMENTS_PATH + "/wallet/wallet-1")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());

        verify(findPaymentsByWalletIdBoundary, never()).execute(any(), anyInt(), anyInt(), any());
    }
}
