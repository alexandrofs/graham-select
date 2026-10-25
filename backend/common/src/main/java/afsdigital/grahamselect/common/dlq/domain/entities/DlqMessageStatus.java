package afsdigital.grahamselect.common.dlq.domain.entities;

public enum DlqMessageStatus {
    PENDING,
    REPROCESSING,
    RESOLVED,
    FAILED
}
