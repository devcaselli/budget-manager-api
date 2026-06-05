package br.com.casellisoftware.budgetmanager.rest.sharing;

import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindActiveShareBySourceBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindAllSharesByOwnerBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.FindShareByIdBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.RevertShareBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.SaveShareBoundary;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareInput;
import br.com.casellisoftware.budgetmanager.application.sharing.boundary.ShareOutput;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareSourceType;
import br.com.casellisoftware.budgetmanager.domain.sharing.ShareStatus;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import br.com.casellisoftware.budgetmanager.rest.sharing.dtos.ShareRequestDto;
import br.com.casellisoftware.budgetmanager.rest.sharing.dtos.ShareResponseDto;
import br.com.casellisoftware.budgetmanager.rest.sharing.mappers.ShareRestMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShareController.class)
@Import(GlobalExceptionHandler.class)
class ShareControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SaveShareBoundary saveShareBoundary;
    @MockitoBean
    private RevertShareBoundary revertShareBoundary;
    @MockitoBean
    private FindShareByIdBoundary findShareByIdBoundary;
    @MockitoBean
    private FindActiveShareBySourceBoundary findActiveShareBySourceBoundary;
    @MockitoBean
    private FindAllSharesByOwnerBoundary findAllSharesByOwnerBoundary;
    @MockitoBean
    private ShareRestMapper mapper;

    @Test
    void create_returns201() throws Exception {
        ShareRequestDto request = new ShareRequestDto(
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                new BigDecimal("100.00"),
                "BRL",
                new BigDecimal("40.00"),
                List.of(new br.com.casellisoftware.budgetmanager.rest.sharing.dtos.ShareQuotaRequestDto("payer-1", null, new BigDecimal("60.00")))
        );
        ShareInput input = new ShareInput(
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                new BigDecimal("100.00"),
                "BRL",
                new BigDecimal("40.00"),
                List.of(),
                "legacy"
        );
        ShareOutput output = new ShareOutput(
                "share-1",
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                new BigDecimal("100.00"),
                new BigDecimal("40.00"),
                new BigDecimal("0.40000000"),
                "BRL",
                ShareStatus.ACTIVE,
                List.of(),
                List.of(),
                Instant.parse("2026-05-14T12:00:00Z"),
                null,
                null
        );
        ShareResponseDto response = new ShareResponseDto(
                "share-1",
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                new BigDecimal("100.00"),
                new BigDecimal("40.00"),
                new BigDecimal("0.40000000"),
                "BRL",
                ShareStatus.ACTIVE,
                List.of(),
                List.of(),
                Instant.parse("2026-05-14T12:00:00Z"),
                null,
                null
        );

        when(mapper.toInput(any(ShareRequestDto.class))).thenReturn(input);
        when(saveShareBoundary.execute(any(ShareInput.class))).thenReturn(output);
        when(mapper.toResponse(output)).thenReturn(response);

        mockMvc.perform(post("/shares")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/shares/share-1"))
                .andExpect(jsonPath("$.id").value("share-1"));
    }

    @Test
    void findAll_returns200() throws Exception {
        ShareOutput output = new ShareOutput(
                "share-1",
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                new BigDecimal("100.00"),
                new BigDecimal("40.00"),
                new BigDecimal("0.40000000"),
                "BRL",
                ShareStatus.ACTIVE,
                List.of(),
                List.of(),
                Instant.parse("2026-05-14T12:00:00Z"),
                null,
                null
        );
        ShareResponseDto response = new ShareResponseDto(
                "share-1",
                "wallet-1",
                ShareSourceType.EXPENSE,
                "expense-1",
                new BigDecimal("100.00"),
                new BigDecimal("40.00"),
                new BigDecimal("0.40000000"),
                "BRL",
                ShareStatus.ACTIVE,
                List.of(),
                List.of(),
                Instant.parse("2026-05-14T12:00:00Z"),
                null,
                null
        );

        when(findAllSharesByOwnerBoundary.execute("legacy")).thenReturn(List.of(output));
        when(mapper.toResponse(output)).thenReturn(response);

        mockMvc.perform(get("/shares"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("share-1"));
    }
}
