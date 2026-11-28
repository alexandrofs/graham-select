package afsdigital.grahamselect.api.cerrado.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "tb_cerrado_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CerradoPreferencesEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "target_acoes_br", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetAcoesBr;

    @Column(name = "target_fiis", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetFiis;

    @Column(name = "target_renda_fixa", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetRendaFixa;

    @Column(name = "target_internacional", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetInternacional;

    @Column(name = "target_cripto", nullable = false, precision = 5, scale = 2)
    private BigDecimal targetCripto;

    @Builder.Default
    @Column(name = "graham_multiplier", nullable = false, precision = 4, scale = 2)
    private BigDecimal grahamMultiplier = new BigDecimal("1.30");

    @Builder.Default
    @Column(name = "graham_filter_enabled", nullable = false)
    private boolean grahamFilterEnabled = true;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void updateTimestamp() {
        this.updatedAt = LocalDateTime.now(ZoneOffset.UTC);
    }
}
