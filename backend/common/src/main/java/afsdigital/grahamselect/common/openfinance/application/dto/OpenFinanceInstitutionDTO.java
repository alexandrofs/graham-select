package afsdigital.grahamselect.common.openfinance.application.dto;

public record OpenFinanceInstitutionDTO(
        String id,
        String name,
        String logoUrl,
        boolean enabled,
        String description
) {}
