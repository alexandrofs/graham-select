package afsdigital.grahamselect.common.user.domain.entities;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDeletionRequest {
    private Long id;
    private Long userId;
    private DeletionStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime completedAt;
    private String failureReason;
}
