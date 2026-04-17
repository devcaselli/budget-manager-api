package br.com.casellisoftware.budgetmanager.rest.advice;

import br.com.casellisoftware.budgetmanager.domain.expense.ExpenseNotFoundException;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration-style tests for {@link GlobalExceptionHandler}, booted via
 * {@code @WebMvcTest} against a dummy controller. This isolates the handler's
 * contract from the real {@code SaveExpenseEntrypoint} (covered by Task 09).
 *
 * <p>What each test locks in:
 * <ul>
 *   <li>HTTP status code (the contract).</li>
 *   <li>{@code Content-Type: application/problem+json} (RFC 7807).</li>
 *   <li>Presence of {@code correlationId} in the body.</li>
 *   <li>Per-exception body shape (errors list, detail text, absence of
 *       stacktrace leakage).</li>
 * </ul>
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.DummyController.class)
@Import({GlobalExceptionHandler.class, GlobalExceptionHandlerTest.DummyController.class})
class GlobalExceptionHandlerTest {

    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void methodArgumentNotValid_returns400_withAllFieldErrors() throws Exception {
        // Both fields blank → two distinct field errors in the same response.
        String body = objectMapper.writeValueAsString(Map.of("name", "", "tag", ""));

        mockMvc.perform(post("/dummy/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.detail").value("Request payload failed validation"))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("name", "tag")))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void httpMessageNotReadable_returns400_withCorrelationId() throws Exception {
        mockMvc.perform(post("/dummy/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Malformed request"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void expenseNotFound_returns404_withMessageInDetail() throws Exception {
        mockMvc.perform(post("/dummy/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Expense not found"))
                .andExpect(jsonPath("$.detail", containsString("abc-123")))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void dataAccess_returns503_withSanitizedBody() throws Exception {
        mockMvc.perform(post("/dummy/data-access"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.title").value("Service unavailable"))
                .andExpect(jsonPath("$.detail").value("Data store is temporarily unavailable"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)))
                // stacktrace-ish content must never leak into the response
                .andExpect(content().string(not(containsString("BOOM internal"))))
                .andExpect(content().string(not(containsString("Exception"))));
    }

    @Test
    void unhandled_returns500_withoutStacktrace() throws Exception {
        mockMvc.perform(post("/dummy/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)))
                .andExpect(content().string(
                        allOf(
                                not(containsString("secret message do not leak")),
                                not(containsString("java.lang"))
                        )));
    }

    @Test
    void optimisticLock_returns409_withCorrelationId() throws Exception {
        mockMvc.perform(post("/dummy/optimistic-lock"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Concurrent modification"))
                .andExpect(jsonPath("$.detail").value("The resource was modified by another request. Please retry."))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void illegalArgument_isNotSpeciallyMappedTo400() throws Exception {
        // The old handler turned any IllegalArgumentException into 400. New rule:
        // IAE is a programmer error → falls through to the generic 500 path.
        mockMvc.perform(post("/dummy/illegal-argument"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal server error"));
    }

    // ---------- Dummy controller to exercise each handler path ----------

    @RestController
    @RequestMapping("/dummy")
    static class DummyController {

        @PostMapping("/validate")
        public String validate(@jakarta.validation.Valid @RequestBody Payload payload) {
            return "ok: " + payload.name();
        }

        @PostMapping("/not-found")
        public String notFound() {
            throw new ExpenseNotFoundException("abc-123");
        }

        @PostMapping("/data-access")
        public String dataAccess() {
            throw new DataAccessResourceFailureException("BOOM internal — connection refused");
        }

        @PostMapping("/boom")
        public String boom() {
            throw new RuntimeException("secret message do not leak");
        }

        @PostMapping("/illegal-argument")
        public String illegalArgument() {
            throw new IllegalArgumentException("should not become 400 anymore");
        }

        @PostMapping("/optimistic-lock")
        public String optimisticLock() {
            throw new OptimisticLockingFailureException("conflict");
        }
    }

    record Payload(
            @NotBlank String name,
            @NotBlank String tag
    ) {
    }
}
