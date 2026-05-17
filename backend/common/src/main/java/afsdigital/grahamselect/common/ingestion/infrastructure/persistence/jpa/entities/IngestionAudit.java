package afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ingestion_audits")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "correlation_id", nullable = false)
    private String correlationId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "upload_date", nullable = false)
    private LocalDateTime uploadDate;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private IngestionStatus status;

    @Column(name = "total_lines", nullable = false)
    private int totalLines;

    @Column(name = "processed_lines", nullable = false)
    private int processedLines;

    @Column(name = "error_lines", nullable = false)
    private int errorLines;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum IngestionStatus {
        PROCESSANDO, SUCESSO, PARCIAL, ERRO
    }
}
