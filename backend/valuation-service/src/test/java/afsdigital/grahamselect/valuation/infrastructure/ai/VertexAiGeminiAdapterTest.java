package afsdigital.grahamselect.valuation.infrastructure.ai;

import afsdigital.grahamselect.common.ai.application.service.AiPayloadValidator;
import afsdigital.grahamselect.common.ai.application.service.DeterministicPortfolioFallbackGenerator;
import afsdigital.grahamselect.common.ai.application.service.PostGenerationValidator;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticBlocks;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticResult;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticSource;
import afsdigital.grahamselect.common.ai.domain.model.MacroClassAllocationSnapshot;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedAssetPosition;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedPortfolioSnapshot;
import afsdigital.grahamselect.common.cerrado.domain.model.CerradoSubClass;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class VertexAiGeminiAdapterTest {

    private static final String EXPECTED_URL =
            "https://us-central1-aiplatform.googleapis.com/v1/projects/kerno-prod/locations/us-central1/publishers/google/models/gemini-2.5-flash:generateContent";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private MockRestServiceServer mockServer;
    private GcpAccessTokenProvider tokenProvider;
    private VertexAiConfigurationProperties properties;
    private VertexAiGeminiAdapter adapter;

    @BeforeEach
    void setUp() {
        properties = new VertexAiConfigurationProperties();
        properties.setProjectId("kerno-prod");
        properties.setLocation("us-central1");
        properties.setModelName("gemini-2.5-flash");
        properties.setTimeout(Duration.ofSeconds(15));
        properties.setTemperature(0.1);

        tokenProvider = mock(GcpAccessTokenProvider.class);

        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();

        adapter = new VertexAiGeminiAdapter(
                builder.build(),
                objectMapper,
                properties,
                tokenProvider,
                new AiPayloadValidator(),
                new PostGenerationValidator(),
                new DeterministicPortfolioFallbackGenerator()
        );
    }

    @Test
    void shouldCallVertexAiEnterpriseEndpointAndReturnValidatedLlmDiagnostic() throws Exception {
        when(tokenProvider.getAccessToken()).thenReturn("gcp-oauth-token");

        AiDiagnosticBlocks validBlocks = new AiDiagnosticBlocks(
                "Carteira com PETR4 em 32,1% e RENDA_FIXA_MANUAL_1 em 67,9%, com HHI de 0,2500.",
                "PETR4 possui nota 8/10 no snapshot.",
                "RENDA_FIXA representa 67,9% frente à meta de 60,0% (desvio de -7,9%).",
                "ACOES_BR está em 32,1% contra alvo de 40,0% (desvio de 7,9%)."
        );
        String vertexResponse = wrapVertexResponse("STOP", objectMapper.writeValueAsString(validBlocks));

        mockServer.expect(once(), requestTo(EXPECTED_URL))
                .andExpect(request -> {
                    assertThat(request.getMethod()).isEqualTo(HttpMethod.POST);
                    assertThat(request.getHeaders().getFirst("Authorization")).isEqualTo("Bearer gcp-oauth-token");
                    String body = request.getBody().toString();
                    assertThat(body).contains("\"temperature\":0.1");
                    assertThat(body).contains("\"responseMimeType\":\"application/json\"");
                    assertThat(body).contains("\"resumoExecutivo\"");
                    assertThat(body).contains("\"racionalDoPlanoDeAporte\"");
                })
                .andRespond(withSuccess(vertexResponse, MediaType.APPLICATION_JSON));

        AiDiagnosticResult result = adapter.analyze(sampleSnapshot());

        mockServer.verify();
        assertThat(result.source()).isEqualTo(AiDiagnosticSource.LLM);
        assertThat(result.modelName()).isEqualTo("gemini-2.5-flash");
        assertThat(result.fallbackReason()).isNull();
        assertThat(result.blocks()).isEqualTo(validBlocks);
    }

    @Test
    void shouldFallbackWithoutRetryWhenLlmViolatesCvmLexicalFilter() throws Exception {
        when(tokenProvider.getAccessToken()).thenReturn("gcp-oauth-token");

        AiDiagnosticBlocks cvmViolation = new AiDiagnosticBlocks(
                "Recomendo comprar PETR4 com 32,1%.",
                "Nota 8/10.",
                "HHI de 0,25.",
                "Desvio de 7,9%."
        );
        mockServer.expect(once(), requestTo(EXPECTED_URL))
                .andRespond(withSuccess(wrapVertexResponse("STOP", objectMapper.writeValueAsString(cvmViolation)), MediaType.APPLICATION_JSON));

        AiDiagnosticResult result = adapter.analyze(sampleSnapshot());

        mockServer.verify();
        assertThat(result.source()).isEqualTo(AiDiagnosticSource.DETERMINISTIC_FALLBACK);
        assertThat(result.fallbackReason()).isEqualTo("CVM_REGULATORY_VIOLATION");
        assertThat(result.blocks().hasAllNonBlankBlocks()).isTrue();
    }

    @Test
    void shouldFallbackWithoutRetryWhenLlmHallucinatesTickerOrNumber() throws Exception {
        when(tokenProvider.getAccessToken()).thenReturn("gcp-oauth-token");

        AiDiagnosticBlocks hallucinated = new AiDiagnosticBlocks(
                "MGLU3 com 45,0% da carteira.",
                "Nota 8/10.",
                "HHI de 0,30.",
                "Desvio de 7,9%."
        );
        mockServer.expect(once(), requestTo(EXPECTED_URL))
                .andRespond(withSuccess(wrapVertexResponse("STOP", objectMapper.writeValueAsString(hallucinated)), MediaType.APPLICATION_JSON));

        AiDiagnosticResult result = adapter.analyze(sampleSnapshot());

        mockServer.verify();
        assertThat(result.source()).isEqualTo(AiDiagnosticSource.DETERMINISTIC_FALLBACK);
        assertThat(result.fallbackReason()).isIn("HALLUCINATED_TICKER", "HALLUCINATED_NUMBER");
    }

    @Test
    void shouldFallbackWithoutRetryOnInvalidVertexHttp200Payload() throws Exception {
        when(tokenProvider.getAccessToken()).thenReturn("gcp-oauth-token");

        mockServer.expect(once(), requestTo(EXPECTED_URL))
                .andRespond(withSuccess(wrapVertexResponse("MAX_TOKENS", "{truncated"), MediaType.APPLICATION_JSON));

        AiDiagnosticResult result = adapter.analyze(sampleSnapshot());

        mockServer.verify();
        assertThat(result.source()).isEqualTo(AiDiagnosticSource.DETERMINISTIC_FALLBACK);
        assertThat(result.fallbackReason()).isEqualTo("INVALID_VERTEX_RESPONSE");
    }

    @Test
    void shouldFallbackWithoutRetryOnHttp503() throws Exception {
        when(tokenProvider.getAccessToken()).thenReturn("gcp-oauth-token");

        mockServer.expect(once(), requestTo(EXPECTED_URL))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        AiDiagnosticResult result = adapter.analyze(sampleSnapshot());

        mockServer.verify();
        assertThat(result.source()).isEqualTo(AiDiagnosticSource.DETERMINISTIC_FALLBACK);
        assertThat(result.fallbackReason()).isEqualTo("VERTEX_API_ERROR");
    }

    @Test
    void shouldFallbackWithoutRetryOnSocketTimeout() throws Exception {
        when(tokenProvider.getAccessToken()).thenReturn("gcp-oauth-token");

        mockServer.expect(once(), requestTo(EXPECTED_URL))
                .andRespond(request -> {
                    throw new ResourceAccessException("Read timed out", new SocketTimeoutException("Read timed out"));
                });

        AiDiagnosticResult result = adapter.analyze(sampleSnapshot());

        mockServer.verify();
        assertThat(result.source()).isEqualTo(AiDiagnosticSource.DETERMINISTIC_FALLBACK);
        assertThat(result.fallbackReason()).isEqualTo("VERTEX_API_ERROR");
    }

    @Test
    void shouldFallbackWithZeroNetworkCallsWhenOAuth2Fails() throws Exception {
        when(tokenProvider.getAccessToken()).thenThrow(new IOException("ADC not found"));

        AiDiagnosticResult result = adapter.analyze(sampleSnapshot());

        mockServer.verify();
        assertThat(result.source()).isEqualTo(AiDiagnosticSource.DETERMINISTIC_FALLBACK);
        assertThat(result.fallbackReason()).isEqualTo("OAUTH2_CREDENTIALS_FAILED");
    }

    @Test
    void shouldFallbackWithZeroNetworkCallsWhenConfigMissingOrSnapshotEmpty() {
        properties.setProjectId("");
        AiDiagnosticResult missingConfig = adapter.analyze(sampleSnapshot());
        assertThat(missingConfig.source()).isEqualTo(AiDiagnosticSource.DETERMINISTIC_FALLBACK);
        assertThat(missingConfig.fallbackReason()).isEqualTo("VERTEX_CONFIG_MISSING");

        AiDiagnosticResult emptySnapshot = adapter.analyze(new SanitizedPortfolioSnapshot(List.of(), List.of(), BigDecimal.ZERO));
        assertThat(emptySnapshot.source()).isEqualTo(AiDiagnosticSource.DETERMINISTIC_FALLBACK);
        assertThat(emptySnapshot.fallbackReason()).isEqualTo("EMPTY_SNAPSHOT");

        mockServer.verify();
    }

    @Test
    void shouldFallbackWithZeroTokenOrNetworkCallsWhenPayloadValidationFails() {
        SanitizedPortfolioSnapshot piiSnapshot = new SanitizedPortfolioSnapshot(
                List.of(
                        new SanitizedAssetPosition(
                                "R$ 100",
                                MacroClass.ACOES_BR,
                                CerradoSubClass.EQUITY_DEFENSIVE,
                                new BigDecimal("100.00"),
                                8,
                                false,
                                null,
                                null,
                                null,
                                null
                        )
                ),
                List.of(),
                new BigDecimal("1.0000")
        );

        AiDiagnosticResult result = adapter.analyze(piiSnapshot);

        assertThat(result.source()).isEqualTo(AiDiagnosticSource.DETERMINISTIC_FALLBACK);
        assertThat(result.fallbackReason()).isEqualTo("PAYLOAD_VALIDATION_FAILED");
        verifyNoInteractions(tokenProvider);
        mockServer.verify();
    }

    @Test
    void shouldFallbackWithoutRetryWhenCandidatesListIsEmpty() throws Exception {
        when(tokenProvider.getAccessToken()).thenReturn("gcp-oauth-token");

        mockServer.expect(once(), requestTo(EXPECTED_URL))
                .andRespond(withSuccess("{\"candidates\":[]}", MediaType.APPLICATION_JSON));

        AiDiagnosticResult result = adapter.analyze(sampleSnapshot());

        mockServer.verify();
        assertThat(result.source()).isEqualTo(AiDiagnosticSource.DETERMINISTIC_FALLBACK);
        assertThat(result.fallbackReason()).isEqualTo("INVALID_VERTEX_RESPONSE");
    }

    @Test
    void shouldFallbackWithoutRetryOnHttp4xx() throws Exception {
        when(tokenProvider.getAccessToken()).thenReturn("gcp-oauth-token");

        mockServer.expect(once(), requestTo(EXPECTED_URL))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST));

        AiDiagnosticResult result = adapter.analyze(sampleSnapshot());

        mockServer.verify();
        assertThat(result.source()).isEqualTo(AiDiagnosticSource.DETERMINISTIC_FALLBACK);
        assertThat(result.fallbackReason()).isEqualTo("VERTEX_API_ERROR");
    }

    @Test
    void shouldSkipLeadingThoughtPartAndParseDiagnosticBlocks() throws Exception {
        when(tokenProvider.getAccessToken()).thenReturn("gcp-oauth-token");

        AiDiagnosticBlocks validBlocks = new AiDiagnosticBlocks(
                "Carteira com PETR4 em 32,1% e RENDA_FIXA_MANUAL_1 em 67,9%, com HHI de 0,2500.",
                "PETR4 possui nota 8/10 no snapshot.",
                "RENDA_FIXA representa 67,9% frente à meta de 60,0% (desvio de -7,9%).",
                "ACOES_BR está em 32,1% contra alvo de 40,0% (desvio de 7,9%)."
        );
        String vertexResponseWithThought = objectMapper.writeValueAsString(Map.of(
                "candidates", List.of(
                        Map.of(
                                "finishReason", "STOP",
                                "content", Map.of(
                                        "parts", List.of(
                                                Map.of("thought", true, "text", "internal reasoning"),
                                                Map.of("text", objectMapper.writeValueAsString(validBlocks))
                                        )
                                )
                        )
                )
        ));

        mockServer.expect(once(), requestTo(EXPECTED_URL))
                .andRespond(withSuccess(vertexResponseWithThought, MediaType.APPLICATION_JSON));

        AiDiagnosticResult result = adapter.analyze(sampleSnapshot());

        mockServer.verify();
        assertThat(result.source()).isEqualTo(AiDiagnosticSource.LLM);
        assertThat(result.blocks()).isEqualTo(validBlocks);
    }

    private String wrapVertexResponse(String finishReason, String text) throws Exception {
        Map<String, Object> body = Map.of(
                "candidates", List.of(
                        Map.of(
                                "finishReason", finishReason,
                                "content", Map.of(
                                        "parts", List.of(Map.of("text", text))
                                )
                        )
                )
        );
        return objectMapper.writeValueAsString(body);
    }

    private SanitizedPortfolioSnapshot sampleSnapshot() {
        return new SanitizedPortfolioSnapshot(
                List.of(
                        new SanitizedAssetPosition(
                                "PETR4",
                                MacroClass.ACOES_BR,
                                CerradoSubClass.EQUITY_DEFENSIVE,
                                new BigDecimal("32.10"),
                                8,
                                false,
                                new BigDecimal("4.50"),
                                new BigDecimal("1.10"),
                                new BigDecimal("12.40"),
                                new BigDecimal("35.00")
                        ),
                        new SanitizedAssetPosition(
                                "RENDA_FIXA_MANUAL_1",
                                MacroClass.RENDA_FIXA,
                                CerradoSubClass.TREASURY_POST_FIXED,
                                new BigDecimal("67.90"),
                                null,
                                false,
                                null,
                                null,
                                null,
                                null
                        )
                ),
                List.of(
                        new MacroClassAllocationSnapshot(
                                MacroClass.ACOES_BR,
                                new BigDecimal("32.10"),
                                new BigDecimal("40.00"),
                                new BigDecimal("7.90")
                        ),
                        new MacroClassAllocationSnapshot(
                                MacroClass.RENDA_FIXA,
                                new BigDecimal("67.90"),
                                new BigDecimal("60.00"),
                                new BigDecimal("-7.90")
                        )
                ),
                new BigDecimal("0.2500")
        );
    }
}
