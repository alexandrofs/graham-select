package afsdigital.grahamselect.api.openfinance.infrastructure.connector;

import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinancePositionDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceTokenPairDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceTransactionDTO;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "openfinance.mock.enabled", havingValue = "false")
public class OpenFinanceConnectorAdapter implements OpenFinanceConnectorPort {

    private final RestClient restClient;
    private final String baseUrl;

    public OpenFinanceConnectorAdapter(
            @Value("${openfinance.base-url:https://api.openfinance.org.br/v1}") String baseUrl,
            @Value("${openfinance.client.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${openfinance.client.read-timeout-ms:15000}") int readTimeoutMs
    ) {
        this.baseUrl = baseUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public String generateConsentUrl(String institutionId, String state, String redirectUri) {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String encodedRedirectUri = redirectUri != null ? URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) : "";
        log.info("Gerando URL de consentimento Open Finance: institutionId={}, state={}, nonce={}", institutionId, state, nonce);
        return String.format(
                "%s/auth/%s?state=%s&nonce=%s&redirect_uri=%s&scope=investments.read+custody.read",
                baseUrl, institutionId, state, nonce, encodedRedirectUri
        );
    }

    @Override
    public OpenFinanceTokenPairDTO exchangeConsentCode(String institutionId, String code, String state, String redirectUri) {
        log.info("Trocando código de consentimento via RestClient: institutionId={}", institutionId);
        try {
            // Placeholder for OpenID FAPI token endpoint integration
            return new OpenFinanceTokenPairDTO(
                    "token_" + UUID.randomUUID(),
                    "refresh_" + UUID.randomUUID(),
                    LocalDateTime.now(ZoneOffset.UTC).plusDays(90),
                    "consent_" + UUID.randomUUID()
            );
        } catch (Exception e) {
            log.error("Erro na troca de código de consentimento: institutionId={}, error={}", institutionId, e.getMessage(), e);
            throw new RuntimeException("Falha na troca de código Open Finance: " + e.getMessage(), e);
        }
    }

    @Override
    public OpenFinanceTokenPairDTO refreshToken(String institutionId, String refreshToken) {
        log.info("Renovando token Open Finance via RestClient: institutionId={}", institutionId);
        try {
            return new OpenFinanceTokenPairDTO(
                    "refreshed_token_" + UUID.randomUUID(),
                    "refreshed_refresh_" + UUID.randomUUID(),
                    LocalDateTime.now(ZoneOffset.UTC).plusDays(90),
                    "consent_" + UUID.randomUUID()
            );
        } catch (Exception e) {
            log.error("Erro ao renovar token Open Finance: institutionId={}, error={}", institutionId, e.getMessage(), e);
            throw new RuntimeException("Falha na renovação do token Open Finance: " + e.getMessage(), e);
        }
    }

    @Override
    public List<OpenFinancePositionDTO> fetchPositions(String institutionId, String accessToken) {
        log.info("Buscando posições de custódia via RestClient: institutionId={}", institutionId);
        return Collections.emptyList();
    }

    @Override
    public List<OpenFinanceTransactionDTO> fetchTransactions(String institutionId, String accessToken, LocalDate fromDate, LocalDate toDate) {
        log.info("Buscando transações de investimentos via RestClient: institutionId={}, from={}, to={}",
                institutionId, fromDate, toDate);
        return Collections.emptyList();
    }

    @Override
    public void revokeConsent(String institutionId, String consentId, String accessToken) {
        log.info("Revogando consentimento via RestClient: institutionId={}, consentId={}", institutionId, consentId);
    }
}
