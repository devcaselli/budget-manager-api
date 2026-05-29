package br.com.casellisoftware.budgetmanager.rest.sync;

import br.com.casellisoftware.budgetmanager.application.sync.boundary.GetSyncPreferenceBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.boundary.SyncIngestBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.boundary.UpdateSyncPreferenceBoundary;
import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncPreferenceOutput;
import br.com.casellisoftware.budgetmanager.application.sync.dto.SyncReport;
import br.com.casellisoftware.budgetmanager.rest.advice.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code @WebMvcTest} for {@link SyncController}. Validates HTTP contract:
 * status codes, JSON body, and validation via {@link GlobalExceptionHandler}.
 *
 * <p>Security is disabled via {@code app.security.enabled=false} in
 * {@code infra/src/test/resources/application.yaml}. {@code AuthenticatedUser}
 * resolves to {@code LEGACY_OWNER_ID} via {@code TestPermitAllSecurityConfiguration}.</p>
 */
@WebMvcTest(controllers = SyncController.class)
@Import(GlobalExceptionHandler.class)
class SyncControllerTest {

    private static final String BASE_PATH = "/sync";
    private static final String UUID_REGEX =
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    private static final String PROBLEM_JSON = MediaType.APPLICATION_PROBLEM_JSON_VALUE;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SyncIngestBoundary syncIngestBoundary;

    @MockitoBean
    private GetSyncPreferenceBoundary getSyncPreferenceBoundary;

    @MockitoBean
    private UpdateSyncPreferenceBoundary updateSyncPreferenceBoundary;

    // ---------- POST /sync/ingest ----------

    @Test
    void syncIngest_happyPath_returns200WithReport() throws Exception {
        when(syncIngestBoundary.execute(anyString()))
                .thenReturn(new SyncReport(5, 2, 1, 0));

        mockMvc.perform(post(BASE_PATH + "/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(5))
                .andExpect(jsonPath("$.skipped").value(2))
                .andExpect(jsonPath("$.fallback").value(1))
                .andExpect(jsonPath("$.errors").value(0));
    }

    @Test
    void syncIngest_emptyResult_returns200WithZeros() throws Exception {
        when(syncIngestBoundary.execute(anyString()))
                .thenReturn(SyncReport.empty());

        mockMvc.perform(post(BASE_PATH + "/ingest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(0))
                .andExpect(jsonPath("$.skipped").value(0))
                .andExpect(jsonPath("$.fallback").value(0))
                .andExpect(jsonPath("$.errors").value(0));
    }

    @Test
    void syncIngest_useCaseThrows_returns500WithoutDetails() throws Exception {
        when(syncIngestBoundary.execute(anyString()))
                .thenThrow(new RuntimeException("internal ingest failure"));

        mockMvc.perform(post(BASE_PATH + "/ingest"))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    // ---------- GET /sync/preferences ----------

    @Test
    void getPreference_happyPath_returns200WithPreference() throws Exception {
        when(getSyncPreferenceBoundary.execute(anyString()))
                .thenReturn(new SyncPreferenceOutput("owner-1", true));

        mockMvc.perform(get(BASE_PATH + "/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value("owner-1"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    void getPreference_disabledOwner_returns200WithEnabledFalse() throws Exception {
        when(getSyncPreferenceBoundary.execute(anyString()))
                .thenReturn(new SyncPreferenceOutput("owner-2", false));

        mockMvc.perform(get(BASE_PATH + "/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    // ---------- PATCH /sync/preferences ----------

    @Test
    void updatePreference_enableTrue_returns200() throws Exception {
        when(updateSyncPreferenceBoundary.execute(anyString(), eq(true)))
                .thenReturn(new SyncPreferenceOutput("owner-1", true));

        mockMvc.perform(patch(BASE_PATH + "/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true));

        verify(updateSyncPreferenceBoundary).execute(anyString(), eq(true));
    }

    @Test
    void updatePreference_enableFalse_returns200() throws Exception {
        when(updateSyncPreferenceBoundary.execute(anyString(), eq(false)))
                .thenReturn(new SyncPreferenceOutput("owner-1", false));

        mockMvc.perform(patch(BASE_PATH + "/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"enabled\": false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void updatePreference_missingEnabledField_returns400() throws Exception {
        mockMvc.perform(patch(BASE_PATH + "/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(PROBLEM_JSON))
                .andExpect(jsonPath("$.correlationId", matchesPattern(UUID_REGEX)));
    }

    @Test
    void updatePreference_emptyBody_returns400() throws Exception {
        mockMvc.perform(patch(BASE_PATH + "/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(""))
                .andExpect(status().isBadRequest());
    }
}
