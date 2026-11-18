package afsdigital.grahamselect.api.openfinance.infrastructure.connector;

import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinancePositionDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceTokenPairDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceTransactionDTO;
import afsdigital.grahamselect.common.openfinance.application.repository.OpenFinanceConnectorPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@ConditionalOnProperty(name = "openfinance.mock.enabled", havingValue = "true", matchIfMissing = true)
public class MockOpenFinanceConnectorAdapter implements OpenFinanceConnectorPort {

    @Override
    public String generateConsentUrl(String institutionId, String state, String redirectUri) {
        String nonce = UUID.randomUUID().toString().replace("-", "");
        log.info("[MOCK] Gerando URL de consentimento sandbox: institutionId={}, state={}, nonce={}", institutionId, state, nonce);
        return String.format(
                "https://sandbox.openfinance.org.br/auth?institution=%s&state=%s&nonce=%s&redirect_uri=%s&scope=investments.read+custody.read",
                institutionId, state, nonce, redirectUri != null ? redirectUri : ""
        );
    }

    @Override
    public OpenFinanceTokenPairDTO exchangeConsentCode(String institutionId, String code, String state, String redirectUri) {
        log.info("[MOCK] Trocando código de consentimento por tokens: institutionId={}, code=[PROTECTED]", institutionId);
        return new OpenFinanceTokenPairDTO(
                "mock_access_token_" + UUID.randomUUID(),
                "mock_refresh_token_" + UUID.randomUUID(),
                LocalDateTime.now(ZoneOffset.UTC).plusDays(90),
                "mock_consent_" + UUID.randomUUID()
        );
    }

    @Override
    public OpenFinanceTokenPairDTO refreshToken(String institutionId, String refreshToken) {
        log.info("[MOCK] Renovando token via refresh token: institutionId={}", institutionId);
        return new OpenFinanceTokenPairDTO(
                "mock_refreshed_access_" + UUID.randomUUID(),
                "mock_refreshed_refresh_" + UUID.randomUUID(),
                LocalDateTime.now(ZoneOffset.UTC).plusDays(90),
                "mock_consent_" + UUID.randomUUID()
        );
    }

    @Override
    public List<OpenFinancePositionDTO> fetchPositions(String institutionId, String accessToken) {
        log.info("[MOCK] Buscando posições de custódia da instituição: institutionId={}", institutionId);
        return List.of(
                new OpenFinancePositionDTO("PETR4", BigDecimal.valueOf(100), BigDecimal.valueOf(36.20), institutionId),
                new OpenFinancePositionDTO("VALE3", BigDecimal.valueOf(50), BigDecimal.valueOf(62.50), institutionId),
                new OpenFinancePositionDTO("ITUB4", BigDecimal.valueOf(200), BigDecimal.valueOf(33.10), institutionId)
        );
    }

    @Override
    public List<OpenFinanceTransactionDTO> fetchTransactions(String institutionId, String accessToken, LocalDate fromDate, LocalDate toDate) {
        log.info("[MOCK] Buscando transações de investimentos: institutionId={}, fromDate={}, toDate={}",
                institutionId, fromDate, toDate);
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        return List.of(
                new OpenFinanceTransactionDTO(
                        "mock-tx-1",
                        "PETR4",
                        now.minusDays(15),
                        BigDecimal.valueOf(100),
                        BigDecimal.valueOf(36.20),
                        "COMPRA",
                        institutionId,
                        "Compra de ações via Home Broker"
                ),
                new OpenFinanceTransactionDTO(
                        "mock-tx-2",
                        "VALE3",
                        now.minusDays(10),
                        BigDecimal.valueOf(50),
                        BigDecimal.valueOf(62.50),
                        "COMPRA",
                        institutionId,
                        "Compra de ações via Home Broker"
                ),
                new OpenFinanceTransactionDTO(
                        "mock-tx-3",
                        "ITUB4",
                        now.minusDays(5),
                        BigDecimal.valueOf(200),
                        BigDecimal.valueOf(33.10),
                        "COMPRA",
                        institutionId,
                        "Compra de ações via Home Broker"
                ),
                new OpenFinanceTransactionDTO(
                        "mock-tx-4",
                        "PETR4",
                        now.minusDays(2),
                        BigDecimal.valueOf(100),
                        BigDecimal.valueOf(0.50),
                        "DIVIDENDO",
                        institutionId,
                        "Pagamento de dividendos em dinheiro"
                )
        );
    }

    @Override
    public void revokeConsent(String institutionId, String consentId, String accessToken) {
        log.info("[MOCK] Revogando consentimento remotamente na instituição: institutionId={}, consentId={}",
                institutionId, consentId);
    }
}
