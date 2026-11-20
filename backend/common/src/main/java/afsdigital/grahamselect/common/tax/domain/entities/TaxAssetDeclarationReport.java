package afsdigital.grahamselect.common.tax.domain.entities;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class TaxAssetDeclarationReport {

    private final int year;
    private final int previousYear;
    private final BigDecimal previousYearTotal;
    private final BigDecimal currentYearTotal;
    private final BigDecimal netVariation;
    private final BigDecimal percentageVariation;
    private final Map<String, BigDecimal> subtotalsByGroup;
    private final List<TaxAssetDeclarationItem> items;

}
