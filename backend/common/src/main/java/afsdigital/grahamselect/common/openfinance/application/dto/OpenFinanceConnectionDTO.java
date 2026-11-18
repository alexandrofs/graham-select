package afsdigital.grahamselect.common.openfinance.application.dto;

import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnection;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnectionStatus;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record OpenFinanceConnectionDTO(
        UUID id,
        String userId,
        UUID profileId,
        String institutionId,
        String institutionName,
        String institutionLogoUrl,
        OpenFinanceConnectionStatus status,
        LocalDateTime consentExpiresAt,
        LocalDateTime lastSyncAt,
        OpenFinanceSyncStatus lastSyncStatus,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static OpenFinanceConnectionDTO fromDomain(OpenFinanceConnection c) {
        if (c == null) {
            return null;
        }
        return new OpenFinanceConnectionDTO(
                c.getId(),
                c.getUserId(),
                c.getProfileId(),
                c.getInstitutionId(),
                c.getInstitutionName(),
                c.getInstitutionLogoUrl(),
                c.getStatus(),
                c.getConsentExpiresAt(),
                c.getLastSyncAt(),
                c.getLastSyncStatus(),
                c.getErrorMessage(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
