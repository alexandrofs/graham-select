package afsdigital.grahamselect.common.openfinance.application.dto;

import java.util.UUID;

public record ConsentUrlResponseDTO(
        UUID connectionId,
        String consentUrl,
        String state
) {}
