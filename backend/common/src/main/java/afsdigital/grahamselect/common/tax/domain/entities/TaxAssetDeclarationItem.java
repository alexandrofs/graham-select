package afsdigital.grahamselect.common.tax.domain.entities;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Set;

@Getter
@Builder
public class TaxAssetDeclarationItem {

    private final String ticker;
    private final String companyName;
    private final String cnpj;
    private final String groupCode;
    private final String itemCode;
    private final String groupDescription;
    private final String locationCode;
    private final BigDecimal quantity;
    private final BigDecimal averagePrice;
    private final BigDecimal previousYearValue;
    private final BigDecimal currentYearValue;
    private final String discrimination;
    private final boolean liquidated;
    private final Set<String> brokers;

}
