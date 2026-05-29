package br.com.casellisoftware.budgetmanager.rest.subscription;

import br.com.casellisoftware.budgetmanager.application.subscription.boundary.DeleteSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindActiveSubscriptionsByMonthBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindAllSubscriptionsBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.FindSubscriptionByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.PatchSubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SaveSubscriptionBoundary;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionInput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionOutput;
import br.com.casellisoftware.budgetmanager.application.subscription.boundary.SubscriptionVersionOutput;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.domain.subscription.exception.SubscriptionNotFoundException;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.PagedSubscriptionResponseDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.SubscriptionPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.SubscriptionRequestDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.SubscriptionResponseDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.dtos.SubscriptionVersionResponseDto;
import br.com.casellisoftware.budgetmanager.rest.subscription.mappers.SubscriptionRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = SubscriptionController.class)
@Import(GlobalExceptionHandler.class)
class SubscriptionControllerTest {

    private static final String SUBSCRIPTIONS_PATH = "/subscriptions";
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SaveSubscriptionBoundary saveSubscriptionBoundary;

    @MockitoBean
    private PatchSubscriptionBoundary patchSubscriptionBoundary;

    @MockitoBean
    private DeleteSubscriptionBoundary deleteSubscriptionBoundary;

    @MockitoBean
    private FindSubscriptionByIdBoundary findSubscriptionByIdBoundary;

    @MockitoBean
    private FindAllSubscriptionsBoundary findAllSubscriptionsBoundary;

    @MockitoBean
    private FindActiveSubscriptionsByMonthBoundary findActiveSubscriptionsByMonthBoundary;

    @MockitoBean
    private SubscriptionRestMapper mapper;

    @Test
    void save_validPayload_returns201WithLocationAndBody() throws Exception {
        SubscriptionRequestDto request = new SubscriptionRequestDto("Netflix", new BigDecimal("55.90"), "BRL", "cc-1");
        SubscriptionInput input = new SubscriptionInput("Netflix", new BigDecimal("55.90"), "BRL", null, null, br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum.NONE, "cc-1");
        SubscriptionOutput output = output("subscription-1", "Netflix");
        SubscriptionResponseDto response = response("subscription-1", "Netflix");

        when(mapper.toInput(any(SubscriptionRequestDto.class))).thenReturn(input);
        when(saveSubscriptionBoundary.execute(input)).thenReturn(output);
        when(mapper.toResponse(output)).thenReturn(response);

        mockMvc.perform(post(SUBSCRIPTIONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/subscriptions/subscription-1"))
                .andExpect(jsonPath("$.id").value("subscription-1"))
                .andExpect(jsonPath("$.description").value("Netflix"))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.startMonth").value("2026-05"))
                .andExpect(jsonPath("$.versions[0].amount").value(55.90));
    }

    @Test
    void save_invalidPayload_returns400WithFieldErrors() throws Exception {
        String body = """
                {
                    "description": "",
                    "amount": -1,
                    "currency": "BR"
                }
                """;

        mockMvc.perform(post(SUBSCRIPTIONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("description", "amount", "currency", "creditCardId")))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(saveSubscriptionBoundary, never()).execute(any());
    }

    @Test
    void save_invalidCurrencyCode_returns400() throws Exception {
        String body = """
                {
                    "description": "Netflix",
                    "amount": 55.90,
                    "currency": "ZZZ",
                    "creditCardId": "cc-1"
                }
                """;

        mockMvc.perform(post(SUBSCRIPTIONS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("currency")));

        verify(saveSubscriptionBoundary, never()).execute(any());
    }

    @Test
    void patch_validPayload_returns200WithBody() throws Exception {
        SubscriptionPatchRequestDto request = new SubscriptionPatchRequestDto("Netflix Premium", new BigDecimal("60.00"), "cc-1");
        PatchSubscriptionInput input = new PatchSubscriptionInput("subscription-1", "Netflix Premium", new BigDecimal("60.00"), "cc-1");
        SubscriptionOutput output = output("subscription-1", "Netflix Premium");
        SubscriptionResponseDto response = response("subscription-1", "Netflix Premium");

        when(mapper.toPatchInput(eq("subscription-1"), any(SubscriptionPatchRequestDto.class))).thenReturn(input);
        when(patchSubscriptionBoundary.execute(input)).thenReturn(output);
        when(mapper.toResponse(output)).thenReturn(response);

        mockMvc.perform(patch(SUBSCRIPTIONS_PATH + "/subscription-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Netflix Premium"));
    }

    @Test
    void patch_blankDescription_returns400() throws Exception {
        String body = """
                {
                    "description": "   ",
                    "creditCardId": "cc-1"
                }
                """;

        mockMvc.perform(patch(SUBSCRIPTIONS_PATH + "/subscription-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("description")));

        verify(patchSubscriptionBoundary, never()).execute(any());
    }

    @Test
    void findById_whenFound_returns200WithBody() throws Exception {
        SubscriptionOutput output = output("subscription-1", "Netflix");
        SubscriptionResponseDto response = response("subscription-1", "Netflix");

        when(findSubscriptionByIdBoundary.execute("subscription-1", "legacy")).thenReturn(output);
        when(mapper.toResponse(output)).thenReturn(response);

        mockMvc.perform(get(SUBSCRIPTIONS_PATH + "/subscription-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("subscription-1"));
    }

    @Test
    void findById_whenMissing_returns404() throws Exception {
        when(findSubscriptionByIdBoundary.execute("missing", "legacy"))
                .thenThrow(new SubscriptionNotFoundException("missing"));

        mockMvc.perform(get(SUBSCRIPTIONS_PATH + "/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Subscription not found"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void findAll_withoutActiveAt_returnsPagedSubscriptions() throws Exception {
        PageResult<SubscriptionOutput> page = new PageResult<>(List.of(output("subscription-1", "Netflix")), 0, 20, 1, 1);
        PagedSubscriptionResponseDto response = new PagedSubscriptionResponseDto(
                List.of(response("subscription-1", "Netflix")),
                0,
                20,
                1,
                1
        );

        when(findAllSubscriptionsBoundary.execute(0, 20, "legacy")).thenReturn(page);
        when(mapper.toPagedResponse(page)).thenReturn(response);

        mockMvc.perform(get(SUBSCRIPTIONS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void findAll_withActiveAt_returnsActiveSubscriptions() throws Exception {
        List<SubscriptionOutput> activeSubscriptions = List.of(output("subscription-1", "Netflix"));
        PagedSubscriptionResponseDto response = new PagedSubscriptionResponseDto(
                List.of(response("subscription-1", "Netflix")),
                0,
                1,
                1,
                1
        );

        when(findActiveSubscriptionsByMonthBoundary.execute(YearMonth.of(2026, 5), "legacy")).thenReturn(activeSubscriptions);
        when(mapper.toPagedResponse(activeSubscriptions)).thenReturn(response);

        mockMvc.perform(get(SUBSCRIPTIONS_PATH).param("activeAt", "2026-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("subscription-1"));
    }

    @Test
    void findAll_withInvalidActiveAt_returns400() throws Exception {
        mockMvc.perform(get(SUBSCRIPTIONS_PATH).param("activeAt", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));

        verify(findActiveSubscriptionsByMonthBoundary, never()).execute(any(), any());
    }

    @Test
    void findAll_withInvalidPage_returns400() throws Exception {
        mockMvc.perform(get(SUBSCRIPTIONS_PATH).param("page", "-1"))
                .andExpect(status().isBadRequest());

        verify(findAllSubscriptionsBoundary, never()).execute(anyInt(), anyInt(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void delete_whenFound_returns204() throws Exception {
        mockMvc.perform(delete(SUBSCRIPTIONS_PATH + "/subscription-1"))
                .andExpect(status().isNoContent());

        verify(deleteSubscriptionBoundary).execute("subscription-1", "legacy");
    }

    private static SubscriptionOutput output(String id, String description) {
        return new SubscriptionOutput(
                id,
                description,
                "BRL",
                YearMonth.of(2026, 5),
                null,
                List.of(new SubscriptionVersionOutput(YearMonth.of(2026, 5), new BigDecimal("55.90")))
        );
    }

    private static SubscriptionResponseDto response(String id, String description) {
        return new SubscriptionResponseDto(
                id,
                description,
                "BRL",
                YearMonth.of(2026, 5),
                null,
                List.of(new SubscriptionVersionResponseDto(YearMonth.of(2026, 5), new BigDecimal("55.90")))
        );
    }
}
