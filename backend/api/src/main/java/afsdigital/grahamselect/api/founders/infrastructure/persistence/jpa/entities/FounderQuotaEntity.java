package afsdigital.grahamselect.api.founders.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_founder_quota")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FounderQuotaEntity {

    @Id
    private Long id;

    @Column(name = "total_slots", nullable = false)
    private int totalSlots;

    @Column(name = "founder_slots_remaining", nullable = false)
    private int founderSlotsRemaining;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
