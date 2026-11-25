package afsdigital.grahamselect.api.founders.web;

import afsdigital.grahamselect.common.founders.application.service.FounderQuotaService;
import afsdigital.grahamselect.common.founders.domain.model.FounderSlotsStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class FoundersControllerTest {

    @Mock
    private FounderQuotaService founderQuotaService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new FoundersController(founderQuotaService)).build();
    }

    @Test
    void shouldReturnActiveSlotsStatus() throws Exception {
        when(founderQuotaService.getSlotsStatus()).thenReturn(new FounderSlotsStatus(250, 250, true));

        mockMvc.perform(get("/api/v1/founders/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").value(250))
                .andExpect(jsonPath("$.remainingSlots").value(250))
                .andExpect(jsonPath("$.isLoteActive").value(true));
    }

    @Test
    void shouldReturnExhaustedSlotsStatus() throws Exception {
        when(founderQuotaService.getSlotsStatus()).thenReturn(new FounderSlotsStatus(250, 0, false));

        mockMvc.perform(get("/api/v1/founders/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").value(250))
                .andExpect(jsonPath("$.remainingSlots").value(0))
                .andExpect(jsonPath("$.isLoteActive").value(false));
    }
}
