package afsdigital.grahamselect.api.ai.web;

import afsdigital.grahamselect.api.common.web.GlobalExceptionHandler;
import afsdigital.grahamselect.common.ai.application.service.exceptions.AiFreeTierNotAllowedException;
import afsdigital.grahamselect.common.ai.application.service.exceptions.AiRateLimitExceededException;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticCacheEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerAiTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyAiController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldReturnProblemDetail429WithLatestDiagnosticWithoutUserId() throws Exception {
        mockMvc.perform(get("/test-ai-rate-limit"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("AI Rate Limit Exceeded"))
                .andExpect(jsonPath("$.dailyCallsUsed").value(5))
                .andExpect(jsonPath("$.dailyLimit").value(5))
                .andExpect(jsonPath("$.monthlyCallsUsed").value(12))
                .andExpect(jsonPath("$.monthlyLimit").value(30))
                .andExpect(jsonPath("$.latestDiagnostic.id").value(42))
                .andExpect(jsonPath("$.latestDiagnostic.userId").doesNotExist())
                .andExpect(jsonPath("$.latestDiagnostic.portfolioStateHash").value("hash-123"))
                .andExpect(jsonPath("$.latestDiagnostic.jsonPayload").value("{\"resumoExecutivo\":\"cached\"}"));
    }

    @Test
    void shouldReturnProblemDetail429WhenLatestDiagnosticIsNull() throws Exception {
        mockMvc.perform(get("/test-ai-rate-limit-null-cache"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429))
                .andExpect(jsonPath("$.title").value("AI Rate Limit Exceeded"))
                .andExpect(jsonPath("$.latestDiagnostic").doesNotExist());
    }

    @Test
    void shouldReturnProblemDetail403ForFreeTierNotAllowed() throws Exception {
        mockMvc.perform(get("/test-ai-free-tier"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("AI Free Tier Not Allowed"))
                .andExpect(jsonPath("$.detail").value("Plano FREE não possui cota para chamadas LLM."));
    }

    @RestController
    static class DummyAiController {
        @GetMapping("/test-ai-rate-limit")
        public void triggerRateLimit() {
            LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
            AiDiagnosticCacheEntry latest = new AiDiagnosticCacheEntry(
                    42L,
                    7L,
                    "hash-123",
                    "gemini-2.5-flash",
                    "v1",
                    "{\"resumoExecutivo\":\"cached\"}",
                    nowUtc.minusHours(1),
                    nowUtc.plusHours(23)
            );
            throw new AiRateLimitExceededException(
                    "Limite de análises de IA atingido (5/5 diário, 12/30 mensal).",
                    latest,
                    5,
                    5,
                    12,
                    30
            );
        }

        @GetMapping("/test-ai-rate-limit-null-cache")
        public void triggerRateLimitWithoutCache() {
            throw new AiRateLimitExceededException(
                    "Limite de análises de IA atingido (5/5 diário, 5/30 mensal).",
                    null,
                    5,
                    5,
                    5,
                    30
            );
        }

        @GetMapping("/test-ai-free-tier")
        public void triggerFreeTier() {
            throw new AiFreeTierNotAllowedException("Plano FREE não possui cota para chamadas LLM.");
        }
    }
}
