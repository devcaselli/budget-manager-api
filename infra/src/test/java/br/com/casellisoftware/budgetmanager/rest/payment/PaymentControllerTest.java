package br.com.casellisoftware.budgetmanager.rest.payment;

import br.com.casellisoftware.budgetmanager.application.payment.boundary.PayExpenseBoundary;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PayExpenseInput;
import br.com.casellisoftware.budgetmanager.application.payment.boundary.PaymentOutput;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import br.com.casellisoftware.budgetmanager.rest.payment.dtos.PayRequestDto;
import br.com.casellisoftware.budgetmanager.rest.payment.dtos.PaymentRequestDto;
import br.com.casellisoftware.budgetmanager.rest.payment.mappers.PaymentRestMapper;
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
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@Import(GlobalExceptionHandler.class)
class PaymentControllerTest {

    private static final String PAY_PATH = "/pay";
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final Instant PAYMENT_DATE = Instant.parse("2026-04-10T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PayExpenseBoundary payExpenseBoundary;

    @MockitoBean
    private PaymentRestMapper mapper;

    @Test
    void pay_validPayload_returns201WithLocation() throws Exception {
        PayRequestDto request = validRequest();
        PayExpenseInput input = new PayExpenseInput(
                Money.of("10.50"),
                PAYMENT_DATE,
                "coffee",
                "expense-1",
                "wallet-1",
                "bullet-1"
        );
        PaymentOutput output = new PaymentOutput(
                "payment-1",
                Money.of("10.50"),
                PAYMENT_DATE,
                "coffee",
                "expense-1",
                "wallet-1",
                "bullet-1"
        );

        when(mapper.toPayExpenseInput(any(PayRequestDto.class), any())).thenReturn(input);
        when(payExpenseBoundary.execute(input)).thenReturn(output);

        mockMvc.perform(post(PAY_PATH)
                        .param("walletId", "wallet-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/payments/payment-1"));

        ArgumentCaptor<PayExpenseInput> captor = ArgumentCaptor.forClass(PayExpenseInput.class);
        verify(payExpenseBoundary).execute(captor.capture());
        assertThat(captor.getValue()).isEqualTo(input);
    }

    @Test
    void pay_missingWalletId_returns400() throws Exception {
        mockMvc.perform(post(PAY_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest())))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(payExpenseBoundary, never()).execute(any());
    }

    @Test
    void pay_invalidPayload_returns400WithFieldErrors() throws Exception {
        String body = """
                {
                    "payment": {
                        "amount": -1,
                        "currency": "BR",
                        "paymentDate": "2099-01-01T00:00:00Z"
                    },
                    "bulletId": "",
                    "expenseId": ""
                }
                """;

        mockMvc.perform(post(PAY_PATH)
                        .param("walletId", "wallet-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder(
                        "payment.amount",
                        "payment.currency",
                        "payment.paymentDate",
                        "bulletId",
                        "expenseId"
                )))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(payExpenseBoundary, never()).execute(any());
    }

    @Test
    void pay_missingPayment_returns400WithFieldError() throws Exception {
        String body = """
                {
                    "bulletId": "bullet-1",
                    "expenseId": "expense-1"
                }
                """;

        mockMvc.perform(post(PAY_PATH)
                        .param("walletId", "wallet-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[?(@.field == 'payment')]").exists())
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(payExpenseBoundary, never()).execute(any());
    }

    private static PayRequestDto validRequest() {
        return new PayRequestDto(
                new PaymentRequestDto(new BigDecimal("10.50"), "BRL", PAYMENT_DATE, "coffee"),
                "bullet-1",
                "expense-1"
        );
    }
}
