package afsdigital.grahamselect.common.domain.entities;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExcludedTicker(
    String ticker,
    String reason,
    String detail
) {}
