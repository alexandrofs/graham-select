package afsdigital.grahamselect.common.tax.application.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record TaxAssetDeclarationReportDTO(
        int year,
        int previousYear,

        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal previousYearTotal,

        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal currentYearTotal,

        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal netVariation,

        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal percentageVariation,

        Map<String, BigDecimal> subtotalsByGroup,

        List<TaxAssetDeclarationItemDTO> items
) {
}
