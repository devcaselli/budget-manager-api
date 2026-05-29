package br.com.casellisoftware.budgetmanager.rest.payer;

import br.com.casellisoftware.budgetmanager.application.payer.boundary.DeletePayerByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.FindAllPayersBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.FindPayerByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PatchPayerBoundary;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerInput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerOutput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.PayerPatchInput;
import br.com.casellisoftware.budgetmanager.application.payer.boundary.SavePayerBoundary;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerNotFoundException;
import br.com.casellisoftware.budgetmanager.domain.payer.PayerType;
import br.com.casellisoftware.budgetmanager.domain.shared.Money;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import br.com.casellisoftware.budgetmanager.rest.payer.dtos.PayerPatchRequestDto;
import br.com.casellisoftware.budgetmanager.rest.payer.dtos.PayerRequestDto;
import br.com.casellisoftware.budgetmanager.rest.payer.dtos.PayerResponseDto;
import br.com.casellisoftware.budgetmanager.rest.payer.mappers.PayerRestMapper;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
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

@WebMvcTest(controllers = PayerController.class)
@Import(GlobalExceptionHandler.class)
class PayerControllerTest {

    private static final String PAYERS_PATH = "/payers";
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SavePayerBoundary savePayerBoundary;

    @MockitoBean
    private PatchPayerBoundary patchPayerBoundary;

    @MockitoBean
    private FindPayerByIdBoundary findPayerByIdBoundary;

    @MockitoBean
    private FindAllPayersBoundary findAllPayersBoundary;

    @MockitoBean
    private DeletePayerByIdBoundary deletePayerByIdBoundary;

    @MockitoBean
    private PayerRestMapper mapper;

    @Test
    void save_validPayload_returns201WithLocationAndBody() throws Exception {
        PayerRequestDto request = new PayerRequestDto("Joao", PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10));
        PayerInput input = new PayerInput("Joao", PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10));
        PayerOutput output = output("payer-1", "Joao", false);
        PayerResponseDto response = response("payer-1", "Joao", false);
        when(mapper.payerRequestDtoToPayerInput(any(PayerRequestDto.class))).thenReturn(input);
        when(savePayerBoundary.execute(any(PayerInput.class))).thenReturn(output);
        when(mapper.payerOutputToPayerResponseDto(output)).thenReturn(response);

        mockMvc.perform(post(PAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/payers/payer-1"))
                .andExpect(jsonPath("$.id").value("payer-1"))
                .andExpect(jsonPath("$.amountDue").value(10.00))
                .andExpect(jsonPath("$.currency").value("BRL"));

        ArgumentCaptor<PayerInput> captor = ArgumentCaptor.forClass(PayerInput.class);
        verify(savePayerBoundary).execute(captor.capture());
        assertThat(captor.getValue().name()).isEqualTo("Joao");
    }

    @Test
    void save_invalidPayload_returns400() throws Exception {
        String body = """
                {
                  "name": " ",
                  "type": null,
                  "paymentDate": null
                }
                """;

        mockMvc.perform(post(PAYERS_PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Validation failed"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));

        verify(savePayerBoundary, never()).execute(any());
    }

    @Test
    void findById_happyPath_returns200() throws Exception {
        PayerOutput output = output("payer-1", "Joao", false);
        when(findPayerByIdBoundary.findById(eq("payer-1"), anyString())).thenReturn(output);
        when(mapper.payerOutputToPayerResponseDto(output)).thenReturn(response("payer-1", "Joao", false));

        mockMvc.perform(get(PAYERS_PATH + "/payer-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("payer-1"))
                .andExpect(jsonPath("$.deleted").value(false));
    }

    @Test
    void findById_missing_returns404() throws Exception {
        when(findPayerByIdBoundary.findById(eq("missing"), anyString()))
                .thenThrow(new PayerNotFoundException("missing"));

        mockMvc.perform(get(PAYERS_PATH + "/missing"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE))
                .andExpect(jsonPath("$.title").value("Payer not found"));
    }

    @Test
    void findAll_returnsList() throws Exception {
        PayerOutput first = output("payer-1", "Joao", false);
        PayerOutput second = output("payer-2", "Maria", false);
        when(findAllPayersBoundary.execute(anyString())).thenReturn(List.of(first, second));
        when(mapper.payerOutputToPayerResponseDto(first)).thenReturn(response("payer-1", "Joao", false));
        when(mapper.payerOutputToPayerResponseDto(second)).thenReturn(response("payer-2", "Maria", false));

        mockMvc.perform(get(PAYERS_PATH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", containsInAnyOrder("payer-1", "payer-2")));
    }

    @Test
    void patch_validPayload_returns200() throws Exception {
        PayerPatchRequestDto request = new PayerPatchRequestDto("Maria", null, null, null, null);
        PayerPatchInput input = new PayerPatchInput(
                Optional.of("Maria"),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty());
        PayerOutput output = output("payer-1", "Maria", false);
        when(mapper.payerPatchRequestDtoToPayerPatchInput(any(PayerPatchRequestDto.class))).thenReturn(input);
        when(patchPayerBoundary.execute(eq("payer-1"), eq(input), anyString())).thenReturn(output);
        when(mapper.payerOutputToPayerResponseDto(output)).thenReturn(response("payer-1", "Maria", false));

        mockMvc.perform(patch(PAYERS_PATH + "/payer-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Maria"));
    }

    @Test
    void deleteById_returns204() throws Exception {
        mockMvc.perform(delete(PAYERS_PATH + "/payer-1"))
                .andExpect(status().isNoContent());

        verify(deletePayerByIdBoundary).execute(eq("payer-1"), anyString());
    }

    @Test
    void deleteById_missingIsStillNoContentBecauseDeleteIsIdempotent() throws Exception {
        mockMvc.perform(delete(PAYERS_PATH + "/missing"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteById_boundaryNotFoundStillMaps404IfRaised() throws Exception {
        doThrow(new PayerNotFoundException("missing"))
                .when(deletePayerByIdBoundary).execute(eq("missing"), anyString());

        mockMvc.perform(delete(PAYERS_PATH + "/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Payer not found"));
    }

    private static PayerOutput output(String id, String name, boolean deleted) {
        return new PayerOutput(id, name, PayerType.STANDING, null, null, LocalDate.of(2026, 5, 10),
                Money.of("10.00"), Money.of("10.00"), Money.of("10.00"), deleted);
    }

    private static PayerResponseDto response(String id, String name, boolean deleted) {
        return new PayerResponseDto(
                id,
                name,
                PayerType.STANDING,
                null,
                null,
                LocalDate.of(2026, 5, 10),
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                new BigDecimal("10.00"),
                "BRL",
                deleted);
    }
}
