package afsdigital.grahamselect.common.tax.application.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.util.List;

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

        List<TaxAssetDeclarationItemDTO> items
) {
}
