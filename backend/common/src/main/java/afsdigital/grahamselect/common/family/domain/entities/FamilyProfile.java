package afsdigital.grahamselect.common.family.domain.entities;

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
@ToString(exclude = "cpfDigits")
public class FamilyProfile {
    private UUID id;
    private String masterUserId;
    private String cpfDigits;
    private String name;
    private String relationship;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

