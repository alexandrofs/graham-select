package afsdigital.grahamselect.common.openfinance.application.dto;

import java.time.LocalDateTime;

public record OpenFinanceTokenPairDTO(
        String accessToken,
        String refreshToken,
        LocalDateTime expiresAt,
        String consentId
) {
    @Override
    public String toString() {
        return "OpenFinanceTokenPairDTO[accessToken=***, refreshToken=***, expiresAt=" + expiresAt + ", consentId=" + consentId + "]";
    }
}
