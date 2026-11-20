package afsdigital.grahamselect.common.tax.application.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;

public record TaxAssetDeclarationItemDTO(
        String ticker,
        String companyName,
        String cnpj,
        String groupCode,
        String itemCode,
        String groupDescription,
        String locationCode,

        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal quantity,

        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal averagePrice,

        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal previousYearValue,

        @JsonSerialize(using = ToStringSerializer.class)
        BigDecimal currentYearValue,

        String discrimination,
        boolean liquidated
) {
}
