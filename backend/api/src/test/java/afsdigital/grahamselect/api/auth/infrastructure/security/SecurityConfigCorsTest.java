package afsdigital.grahamselect.api.auth.infrastructure.security;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class SecurityConfigCorsTest extends BaseRepositoryIT {

    private static final String OFFICIAL_ORIGIN = "https://kerno.afsdigital.com.br";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Preflight OPTIONS com origem oficial Kerno retorna headers CORS com Allow-Credentials=true")
    void shouldAllowPreflightFromOfficialKernoOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/notifications/stream")
                        .header(HttpHeaders.ORIGIN, OFFICIAL_ORIGIN)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization,Cache-Control"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, OFFICIAL_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @Test
    @DisplayName("Requisição GET com origem oficial Kerno retorna Access-Control-Allow-Origin correspondente")
    void shouldAllowGetFromOfficialKernoOrigin() throws Exception {
        mockMvc.perform(get("/actuator/health")
                        .header(HttpHeaders.ORIGIN, OFFICIAL_ORIGIN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, OFFICIAL_ORIGIN))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://graham.afsdigital.com.br",
            "https://graham-select.afsdigital.com.br",
            "https://graham-select-frontend.onrender.com",
            "https://evil.example.com"
    })
    @DisplayName("Origens não listadas (incluindo domínios legados) são rejeitadas pelo CORS")
    void shouldRejectUnlistedOrigins(String unlistedOrigin) throws Exception {
        mockMvc.perform(options("/api/v1/notifications/stream")
                        .header(HttpHeaders.ORIGIN, unlistedOrigin)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));

        mockMvc.perform(get("/actuator/health")
                        .header(HttpHeaders.ORIGIN, unlistedOrigin))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }
}
