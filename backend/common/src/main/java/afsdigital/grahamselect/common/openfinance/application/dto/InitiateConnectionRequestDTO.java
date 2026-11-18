package afsdigital.grahamselect.common.openfinance.application.dto;

import java.util.UUID;

public record InitiateConnectionRequestDTO(
        String institutionId,
        UUID profileId
) {}
