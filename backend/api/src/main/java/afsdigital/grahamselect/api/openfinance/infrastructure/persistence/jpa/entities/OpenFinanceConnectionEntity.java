package afsdigital.grahamselect.api.openfinance.infrastructure.persistence.jpa.entities;

import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnection;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceConnectionStatus;
import afsdigital.grahamselect.common.openfinance.domain.entities.OpenFinanceSyncStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "open_finance_connections")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpenFinanceConnectionEntity implements Persistable<UUID> {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(length = 36, nullable = false)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "profile_id", length = 36)
    private UUID profileId;

    @Column(name = "institution_id", nullable = false, length = 50)
    private String institutionId;

    @Column(name = "institution_name", nullable = false, length = 100)
    private String institutionName;

    @Column(name = "institution_logo_url")
    private String institutionLogoUrl;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "consent_id", length = 100)
    private String consentId;

    @Column(name = "encrypted_access_token", length = 2048)
    private String encryptedAccessToken;

    @Column(name = "encrypted_refresh_token", length = 2048)
    private String encryptedRefreshToken;

    @Column(name = "consent_expires_at")
    private LocalDateTime consentExpiresAt;

    @Column(name = "last_sync_at")
    private LocalDateTime lastSyncAt;

    @Column(name = "last_sync_status", length = 30)
    private String lastSyncStatus;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    void markNotNew() {
        this.isNew = false;
    }

    public OpenFinanceConnection toDomain() {
        return OpenFinanceConnection.builder()
                .id(id)
                .userId(userId)
                .profileId(profileId)
                .institutionId(institutionId)
                .institutionName(institutionName)
                .institutionLogoUrl(institutionLogoUrl)
                .status(status != null ? OpenFinanceConnectionStatus.valueOf(status) : null)
                .consentId(consentId)
                .encryptedAccessToken(encryptedAccessToken)
                .encryptedRefreshToken(encryptedRefreshToken)
                .consentExpiresAt(consentExpiresAt)
                .lastSyncAt(lastSyncAt)
                .lastSyncStatus(lastSyncStatus != null ? OpenFinanceSyncStatus.valueOf(lastSyncStatus) : null)
                .errorMessage(errorMessage)
                .createdAt(createdAt)
                .updatedAt(updatedAt)
                .build();
    }

    public static OpenFinanceConnectionEntity fromDomain(OpenFinanceConnection domain) {
        if (domain == null) {
            return null;
        }
        return OpenFinanceConnectionEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .profileId(domain.getProfileId())
                .institutionId(domain.getInstitutionId())
                .institutionName(domain.getInstitutionName())
                .institutionLogoUrl(domain.getInstitutionLogoUrl())
                .status(domain.getStatus() != null ? domain.getStatus().name() : null)
                .consentId(domain.getConsentId())
                .encryptedAccessToken(domain.getEncryptedAccessToken())
                .encryptedRefreshToken(domain.getEncryptedRefreshToken())
                .consentExpiresAt(domain.getConsentExpiresAt())
                .lastSyncAt(domain.getLastSyncAt())
                .lastSyncStatus(domain.getLastSyncStatus() != null ? domain.getLastSyncStatus().name() : null)
                .errorMessage(domain.getErrorMessage())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }
}
