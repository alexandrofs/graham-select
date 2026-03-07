package afsdigital.grahamselect.common.domain.entities;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@AllArgsConstructor
@NoArgsConstructor
@Data
@JsonSerialize(using = FinancialDataKeySerializer.class)
@JsonDeserialize(using = FinancialDataKeyDeserializer.class)
public class FinancialDataKey implements Serializable {

    private String ticker;
    private LocalDate referenceDate;

}
