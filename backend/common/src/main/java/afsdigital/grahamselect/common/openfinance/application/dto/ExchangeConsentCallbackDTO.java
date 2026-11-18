package afsdigital.grahamselect.common.openfinance.application.dto;

import java.util.UUID;

public record ExchangeConsentCallbackDTO(
        UUID connectionId,
        String code,
        String state
) {}
