package afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "allocation_goals")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AllocationGoalReadEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "goal_type", nullable = false, length = 20)
    private String goalType; // "ASSET_CLASS" | "TICKER"

    @Column(name = "target_key", nullable = false, length = 20)
    private String targetKey;

    @Column(name = "target_percentage", nullable = false, precision = 8, scale = 4)
    private BigDecimal targetPercentage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
