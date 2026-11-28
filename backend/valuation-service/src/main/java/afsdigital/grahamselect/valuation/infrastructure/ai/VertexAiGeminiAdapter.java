package afsdigital.grahamselect.valuation.infrastructure.ai;

import afsdigital.grahamselect.common.ai.application.port.AiPortfolioAnalyzerPort;
import afsdigital.grahamselect.common.ai.application.service.AiPayloadValidationException;
import afsdigital.grahamselect.common.ai.application.service.AiPayloadValidator;
import afsdigital.grahamselect.common.ai.application.service.DeterministicPortfolioFallbackGenerator;
import afsdigital.grahamselect.common.ai.application.service.PostGenerationValidator;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticBlocks;
import afsdigital.grahamselect.common.ai.domain.model.AiDiagnosticResult;
import afsdigital.grahamselect.common.ai.domain.model.SanitizedPortfolioSnapshot;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Corporate Google Cloud Vertex AI (Enterprise ZDR) adapter implementing
 * {@link AiPortfolioAnalyzerPort}. Enforces pre-flight LGPD payload validation,
 * 15s rigid timeout, zero synchronous retries, post-generation deterministic
 * anchoring/CVM checks, and local quantitative fallback on any failure.
 */
public class VertexAiGeminiAdapter implements AiPortfolioAnalyzerPort {

    private static final Logger log = LoggerFactory.getLogger(VertexAiGeminiAdapter.class);

    private static final String SYSTEM_INSTRUCTION = """
            Você é um analista quantitativo educacional de carteiras de investimentos no Brasil.
            Produza um diagnóstico estritamente analítico e educacional em português (pt-BR), \
            dividido nos 4 blocos do schema JSON.
            Regras obrigatórias:
            1) Nunca recomende, sugira ou exorte compra ou venda de ativos, nem prometa ou garanta retorno/lucro.
            2) Cite exclusivamente tickers e valores numéricos presentes no snapshot JSON fornecido (sem inventar ativos ou números).
            3) Nunca mencione valores monetários absolutos (R$, US$, BRL, USD) ou dados pessoais.
            4) Baseie toda análise nos pesos percentuais, notas (escala /10), múltiplos, margem de Graham, HHI e desvios de macroclasse.""";

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final VertexAiConfigurationProperties properties;
    private final GcpAccessTokenProvider accessTokenProvider;
    private final AiPayloadValidator payloadValidator;
    private final PostGenerationValidator postGenerationValidator;
    private final DeterministicPortfolioFallbackGenerator fallbackGenerator;

    public VertexAiGeminiAdapter(
            RestClient restClient,
            ObjectMapper objectMapper,
            VertexAiConfigurationProperties properties,
            GcpAccessTokenProvider accessTokenProvider,
            AiPayloadValidator payloadValidator,
            PostGenerationValidator postGenerationValidator,
            DeterministicPortfolioFallbackGenerator fallbackGenerator
    ) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.accessTokenProvider = accessTokenProvider;
        this.payloadValidator = payloadValidator;
        this.postGenerationValidator = postGenerationValidator;
        this.fallbackGenerator = fallbackGenerator;
    }

    @Override
    public AiDiagnosticResult analyze(SanitizedPortfolioSnapshot snapshot) {
        if (snapshot == null || snapshot.positions().isEmpty()) {
            log.warn("Skipping Vertex AI call for empty sanitized snapshot; using deterministic fallback");
            return fallbackGenerator.generate(snapshot, "EMPTY_SNAPSHOT");
        }

        if (!properties.isConfigured()) {
            log.error("Vertex AI configuration is missing projectId/location/modelName; using deterministic fallback");
            return fallbackGenerator.generate(snapshot, "VERTEX_CONFIG_MISSING");
        }

        String snapshotJson;
        try {
            snapshotJson = objectMapper.writeValueAsString(snapshot);
            payloadValidator.validate(snapshotJson);
        } catch (JsonProcessingException | AiPayloadValidationException ex) {
            log.error("Pre-flight AI payload serialization/validation failed; aborting without network call", ex);
            return fallbackGenerator.generate(snapshot, "PAYLOAD_VALIDATION_FAILED");
        }

        String accessToken;
        try {
            accessToken = accessTokenProvider.getAccessToken();
        } catch (Exception ex) {
            log.error("Failed to obtain GCP OAuth2 access token for Vertex AI; using deterministic fallback", ex);
            return fallbackGenerator.generate(snapshot, "OAUTH2_CREDENTIALS_FAILED");
        }

        String endpointUrl = properties.buildGenerateContentUrl();
        Map<String, Object> requestBody = buildRequestBody(snapshotJson);

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri(endpointUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + accessToken)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);
        } catch (Exception ex) {
            log.error("Vertex AI generateContent call failed (timeout or HTTP error); using deterministic fallback without retry", ex);
            return fallbackGenerator.generate(snapshot, "VERTEX_API_ERROR");
        }

        AiDiagnosticBlocks blocks;
        try {
            blocks = parseDiagnosticBlocks(responseBody);
        } catch (Exception ex) {
            log.error("Vertex AI response parsing/structure validation failed; using deterministic fallback without retry", ex);
            return fallbackGenerator.generate(snapshot, "INVALID_VERTEX_RESPONSE");
        }

        PostGenerationValidator.ValidationResult validation = postGenerationValidator.validate(blocks, snapshot);
        if (!validation.valid()) {
            log.warn(
                    "Post-generation validation rejected Vertex AI output: reasonCode={} detail={}; using deterministic fallback",
                    validation.reasonCode(),
                    validation.message()
            );
            return fallbackGenerator.generate(snapshot, validation.reasonCode());
        }

        log.info(
                "Vertex AI diagnostic generated and validated successfully: model={} location={}",
                properties.getModelName(),
                properties.getLocation()
        );
        return AiDiagnosticResult.fromLlm(blocks, properties.getModelName());
    }

    private Map<String, Object> buildRequestBody(String snapshotJson) {
        String userPrompt = "Analise o snapshot percentual sanitizado abaixo e preencha os 4 blocos do diagnóstico:\n"
                + snapshotJson;

        Map<String, Object> responseSchema = Map.of(
                "type", "OBJECT",
                "required", List.of(
                        "resumoExecutivo",
                        "pontosFortes",
                        "alertasDeRiscoEConcentracao",
                        "racionalDoPlanoDeAporte"
                ),
                "properties", Map.of(
                        "resumoExecutivo", Map.of("type", "STRING"),
                        "pontosFortes", Map.of("type", "STRING"),
                        "alertasDeRiscoEConcentracao", Map.of("type", "STRING"),
                        "racionalDoPlanoDeAporte", Map.of("type", "STRING")
                )
        );

        return Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", SYSTEM_INSTRUCTION))
                ),
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(Map.of("text", userPrompt))
                        )
                ),
                "generationConfig", Map.of(
                        "temperature", properties.getTemperature(),
                        "maxOutputTokens", 2048,
                        "thinkingConfig", Map.of("thinkingBudget", 0),
                        "responseMimeType", "application/json",
                        "responseSchema", responseSchema
                )
        );
    }

    private AiDiagnosticBlocks parseDiagnosticBlocks(String responseBody) throws JsonProcessingException {
        if (responseBody == null || responseBody.isBlank()) {
            throw new IllegalStateException("Vertex AI returned blank response body");
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            throw new IllegalStateException("Vertex AI response contains no candidates");
        }

        JsonNode firstCandidate = candidates.get(0);
        String finishReason = firstCandidate.path("finishReason").asText("");
        if (!"STOP".equals(finishReason)) {
            throw new IllegalStateException("Vertex AI candidate finishReason was not STOP: " + finishReason);
        }

        JsonNode parts = firstCandidate.path("content").path("parts");
        if (!parts.isArray() || parts.isEmpty()) {
            throw new IllegalStateException("Vertex AI candidate contains no content parts");
        }

        String rawBlocksJson = null;
        for (JsonNode part : parts) {
            if (part.path("thought").asBoolean(false)) {
                continue;
            }
            String candidateText = part.path("text").asText(null);
            if (candidateText != null && !candidateText.isBlank()) {
                rawBlocksJson = candidateText;
                break;
            }
        }
        if (rawBlocksJson == null) {
            throw new IllegalStateException("Vertex AI candidate contains no non-thought text part");
        }

        return objectMapper.readValue(rawBlocksJson, AiDiagnosticBlocks.class);
    }
}
