package afsdigital.grahamselect.api.portfolio.web;

import afsdigital.grahamselect.api.portfolio.infrastructure.sse.SseNotificationAdapter;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class NotificationControllerTest extends BaseRepositoryIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SseNotificationAdapter sseNotificationAdapter;

    @Test
    void shouldReturnSseStream() throws Exception {
        SseEmitter emitter = new SseEmitter();
        when(sseNotificationAdapter.createEmitter("user-1")).thenReturn(emitter);

        mockMvc.perform(get("/api/v1/notifications/stream")
                        .with(jwt().jwt(builder -> builder.subject("user-1")))
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnUnauthorizedWhenNoJwt() throws Exception {
        mockMvc.perform(get("/api/v1/notifications/stream")
                        .accept(MediaType.TEXT_EVENT_STREAM_VALUE))
                .andExpect(status().isUnauthorized());
    }
}
