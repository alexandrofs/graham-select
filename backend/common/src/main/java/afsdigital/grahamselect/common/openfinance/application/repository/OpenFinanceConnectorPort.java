package afsdigital.grahamselect.common.openfinance.application.repository;

import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinancePositionDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceTokenPairDTO;
import afsdigital.grahamselect.common.openfinance.application.dto.OpenFinanceTransactionDTO;

import java.time.LocalDate;
import java.util.List;

public interface OpenFinanceConnectorPort {
    String generateConsentUrl(String institutionId, String state, String redirectUri);

    OpenFinanceTokenPairDTO exchangeConsentCode(String institutionId, String code, String state, String redirectUri);

    OpenFinanceTokenPairDTO refreshToken(String institutionId, String refreshToken);

    List<OpenFinancePositionDTO> fetchPositions(String institutionId, String accessToken);

    List<OpenFinanceTransactionDTO> fetchTransactions(String institutionId, String accessToken, LocalDate fromDate, LocalDate toDate);

    void revokeConsent(String institutionId, String consentId, String accessToken);
}
