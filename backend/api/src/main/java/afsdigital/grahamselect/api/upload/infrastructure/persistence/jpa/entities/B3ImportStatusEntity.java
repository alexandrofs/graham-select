package afsdigital.grahamselect.api.upload.infrastructure.persistence.jpa.entities;

import afsdigital.grahamselect.common.upload.domain.model.B3ImportStatusState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "b3_import_status")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class B3ImportStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "correlation_id", nullable = false, unique = true, length = 36)
    private String correlationId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private B3ImportStatusState status;

    @Column(name = "processed_rows", nullable = false)
    private int processedRows;

    @Column(name = "successful_rows", nullable = false)
    private int successfulRows;

    @Column(name = "failed_rows", nullable = false)
    private int failedRows;

    @Column(name = "status_message")
    private String message;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
