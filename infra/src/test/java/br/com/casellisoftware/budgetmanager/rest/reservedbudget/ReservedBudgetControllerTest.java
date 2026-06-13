package br.com.casellisoftware.budgetmanager.rest.reservedbudget;

import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.DeleteReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindActiveReservedBudgetsByMonthBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindAllReservedBudgetsBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.FindReservedBudgetByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.PatchReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.PatchReservedBudgetInput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetInput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetOutput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.ReservedBudgetVersionOutput;
import br.com.casellisoftware.budgetmanager.application.reservedbudget.boundary.SaveReservedBudgetBoundary;
import br.com.casellisoftware.budgetmanager.domain.flag.FlagEnum;
import br.com.casellisoftware.budgetmanager.domain.reservedbudget.ReservedBudgetNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.shared.PageResult;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos.PagedReservedBudgetResponseDto;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos.ReservedBudgetPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos.ReservedBudgetRequestDto;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos.ReservedBudgetResponseDto;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.dtos.ReservedBudgetVersionResponseDto;
import br.com.casellisoftware.budgetmanager.rest.reservedbudget.mappers.ReservedBudgetRestMapper;
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
import static org.mockito.ArgumentMatchers.any;
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

@WebMvcTest(controllers = ReservedBudgetController.class)
@Import(GlobalExceptionHandler.class)
class ReservedBudgetControllerTest {

    private static final String PATH = "/reserved-budgets";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SaveReservedBudgetBoundary saveReservedBudgetBoundary;

    @MockitoBean
    private PatchReservedBudgetBoundary patchReservedBudgetBoundary;

    @MockitoBean
    private DeleteReservedBudgetBoundary deleteReservedBudgetBoundary;

    @MockitoBean
    private FindReservedBudgetByIdBoundary findReservedBudgetByIdBoundary;

    @MockitoBean
    private FindAllReservedBudgetsBoundary findAllReservedBudgetsBoundary;

    @MockitoBean
    private FindActiveReservedBudgetsByMonthBoundary findActiveReservedBudgetsByMonthBoundary;

    @MockitoBean
    private ReservedBudgetRestMapper mapper;

    @Test
    void save_validPayload_returns201WithLocationAndBody() throws Exception {
        ReservedBudgetRequestDto request = new ReservedBudgetRequestDto("Aluguel", "rent", new BigDecimal("2000.00"), "BRL", YearMonth.of(2025, 3));
        ReservedBudgetInput input = new ReservedBudgetInput("Aluguel", "rent", new BigDecimal("2000.00"), "BRL", YearMonth.of(2025, 3), FlagEnum.NONE);
        ReservedBudgetOutput output = output("rb-1", "Aluguel");
        ReservedBudgetResponseDto response = response("rb-1", "Aluguel");

        when(mapper.toInput(any(ReservedBudgetRequestDto.class))).thenReturn(input);
        when(saveReservedBudgetBoundary.execute(input)).thenReturn(output);
        when(mapper.toResponse(output)).thenReturn(response);

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/reserved-budgets/rb-1"))
                .andExpect(jsonPath("$.id").value("rb-1"))
                .andExpect(jsonPath("$.description").value("Aluguel"))
                .andExpect(jsonPath("$.currency").value("BRL"))
                .andExpect(jsonPath("$.startMonth").value("2025-03"))
                .andExpect(jsonPath("$.versions[0].amount").value(2000.00));
    }

    @Test
    void save_invalidPayload_returns400WithFieldErrors() throws Exception {
        String body = """
                {
                    "description": "",
                    "budget": -1,
                    "currency": "BR",
                    "effectiveMonth": null
                }
                """;

        mockMvc.perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("description", "budget", "currency", "effectiveMonth")));

        verify(saveReservedBudgetBoundary, never()).execute(any());
    }

    @Test
    void patch_validPayload_returns200WithBody() throws Exception {
        ReservedBudgetPatchRequestDto request = new ReservedBudgetPatchRequestDto("Aluguel apto", new BigDecimal("1500.00"));
        PatchReservedBudgetInput input = new PatchReservedBudgetInput("rb-1", "Aluguel apto", null, new BigDecimal("1500.00"), FlagEnum.NONE);
        ReservedBudgetOutput output = output("rb-1", "Aluguel apto");
        ReservedBudgetResponseDto response = response("rb-1", "Aluguel apto");

        when(mapper.toPatchInput(eq("rb-1"), any(ReservedBudgetPatchRequestDto.class))).thenReturn(input);
        when(patchReservedBudgetBoundary.execute(input)).thenReturn(output);
        when(mapper.toResponse(output)).thenReturn(response);

        mockMvc.perform(patch(PATH + "/rb-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("Aluguel apto"));
    }

    @Test
    void findById_whenFound_returns200WithBody() throws Exception {
        ReservedBudgetOutput output = output("rb-1", "Aluguel");
        ReservedBudgetResponseDto response = response("rb-1", "Aluguel");

        when(findReservedBudgetByIdBoundary.execute("rb-1", "legacy")).thenReturn(output);
        when(mapper.toResponse(output)).thenReturn(response);

        mockMvc.perform(get(PATH + "/rb-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("rb-1"));
    }

    @Test
    void findById_whenMissing_returns404() throws Exception {
        when(findReservedBudgetByIdBoundary.execute("missing", "legacy"))
                .thenThrow(new ReservedBudgetNotFoundException("missing"));

        mockMvc.perform(get(PATH + "/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON));
    }

    @Test
    void findAll_withoutActiveAt_returnsPaged() throws Exception {
        PageResult<ReservedBudgetOutput> page = new PageResult<>(List.of(output("rb-1", "Aluguel")), 0, 20, 1, 1);
        PagedReservedBudgetResponseDto response = new PagedReservedBudgetResponseDto(List.of(response("rb-1", "Aluguel")), 0, 20, 1, 1);

        when(findAllReservedBudgetsBoundary.execute(0, 20, "legacy")).thenReturn(page);
        when(mapper.toPagedResponse(page)).thenReturn(response);

        mockMvc.perform(get(PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void findAll_withActiveAt_returnsActive() throws Exception {
        List<ReservedBudgetOutput> active = List.of(output("rb-1", "Aluguel"));
        PagedReservedBudgetResponseDto response = new PagedReservedBudgetResponseDto(List.of(response("rb-1", "Aluguel")), 0, 1, 1, 1);

        when(findActiveReservedBudgetsByMonthBoundary.execute(YearMonth.of(2025, 3), "legacy")).thenReturn(active);
        when(mapper.toPagedResponse(active)).thenReturn(response);

        mockMvc.perform(get(PATH).param("activeAt", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value("rb-1"));
    }

    @Test
    void delete_returns204AndDelegatesSoftDelete() throws Exception {
        mockMvc.perform(delete(PATH + "/rb-1"))
                .andExpect(status().isNoContent());

        verify(deleteReservedBudgetBoundary).execute("rb-1", "legacy");
    }

    private static ReservedBudgetOutput output(String id, String description) {
        return new ReservedBudgetOutput(
                id,
                description,
                "rent",
                "BRL",
                YearMonth.of(2025, 3),
                List.of(new ReservedBudgetVersionOutput(YearMonth.of(2025, 3), new BigDecimal("2000.00"))),
                false,
                FlagEnum.NONE
        );
    }

    private static ReservedBudgetResponseDto response(String id, String description) {
        return new ReservedBudgetResponseDto(
                id,
                description,
                "rent",
                "BRL",
                YearMonth.of(2025, 3),
                List.of(new ReservedBudgetVersionResponseDto(YearMonth.of(2025, 3), new BigDecimal("2000.00"))),
                false,
                FlagEnum.NONE
        );
    }
}
