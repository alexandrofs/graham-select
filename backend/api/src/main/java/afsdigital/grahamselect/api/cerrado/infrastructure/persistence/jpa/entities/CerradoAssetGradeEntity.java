package afsdigital.grahamselect.api.cerrado.infrastructure.persistence.jpa.entities;

import afsdigital.grahamselect.common.cerrado.domain.model.CerradoSubClass;
import afsdigital.grahamselect.common.cerrado.domain.model.MacroClass;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "tb_cerrado_asset_grade")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CerradoAssetGradeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "ticker", nullable = false, length = 50)
    private String ticker;

    @Enumerated(EnumType.STRING)
    @Column(name = "macro_class", nullable = false, length = 30)
    private MacroClass macroClass;

    @Enumerated(EnumType.STRING)
    @Column(name = "sub_class", nullable = false, length = 50)
    private CerradoSubClass subClass;

    @Min(1)
    @Max(10)
    @Builder.Default
    @Column(name = "quality_grade", nullable = false)
    private int qualityGrade = 6;

    @Column(name = "custom_risk_ceiling", precision = 5, scale = 2)
    private BigDecimal customRiskCeiling;

    @Builder.Default
    @Column(name = "quarantine_corporate_event", nullable = false)
    private boolean quarantineCorporateEvent = false;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
