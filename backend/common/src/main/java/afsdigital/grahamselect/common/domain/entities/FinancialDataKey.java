package afsdigital.grahamselect.common.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class FinancialDataKey implements Serializable {

    private String ticker;
    private LocalDate referenceDate;

}
