package afsdigital.grahamselect.api.admin.web;

import afsdigital.grahamselect.common.dividends.application.dto.CorporateEventSyncSummaryDTO;
import afsdigital.grahamselect.common.dividends.application.usecase.SyncCorporateEventsUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminDividendCrawlerControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SyncCorporateEventsUseCase syncCorporateEventsUseCase;

    @InjectMocks
    private AdminDividendCrawlerController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/v1/admin/crawler/dividends/sync deve executar sincronização e retornar 200 OK")
    void shouldTriggerSyncSuccessfully() throws Exception {
        CorporateEventSyncSummaryDTO summary = new CorporateEventSyncSummaryDTO(
                15, 8, 3, 4, Instant.now(), "SUCCESS"
        );

        when(syncCorporateEventsUseCase.execute()).thenReturn(summary);

        mockMvc.perform(post("/api/v1/admin/crawler/dividends/sync")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tickersProcessed").value(15))
                .andExpect(jsonPath("$.newDividends").value(8))
                .andExpect(jsonPath("$.updatedDividends").value(3))
                .andExpect(jsonPath("$.newFacts").value(4))
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    @DisplayName("Endpoint triggerSync deve ser protegido com a anotação @RequireAdmin")
    void shouldBeAnnotatedWithRequireAdmin() throws NoSuchMethodException {
        var method = AdminDividendCrawlerController.class.getMethod("triggerSync", org.springframework.security.core.Authentication.class);
        org.junit.jupiter.api.Assertions.assertTrue(
                method.isAnnotationPresent(afsdigital.grahamselect.api.auth.infrastructure.security.RequireAdmin.class),
                "O endpoint deve conter a anotação @RequireAdmin"
        );
    }
}
