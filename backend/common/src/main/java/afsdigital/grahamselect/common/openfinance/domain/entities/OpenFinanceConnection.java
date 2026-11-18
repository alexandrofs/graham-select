package afsdigital.grahamselect.common.openfinance.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"encryptedAccessToken", "encryptedRefreshToken"})
public class OpenFinanceConnection {
    private UUID id;
    private String userId;
    private UUID profileId;
    private String institutionId;
    private String institutionName;
    private String institutionLogoUrl;
    private OpenFinanceConnectionStatus status;
    private String consentId;
    private String encryptedAccessToken;
    private String encryptedRefreshToken;
    private LocalDateTime consentExpiresAt;
    private LocalDateTime lastSyncAt;
    private OpenFinanceSyncStatus lastSyncStatus;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
