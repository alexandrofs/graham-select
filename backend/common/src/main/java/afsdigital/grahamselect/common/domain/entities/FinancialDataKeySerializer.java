package afsdigital.grahamselect.common.domain.entities;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class FinancialDataKeySerializer extends JsonSerializer<FinancialDataKey> {

    private static final DateTimeFormatter DEFAULT_DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    @Override
    public void serialize(FinancialDataKey value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        gen.writeStartObject();

        if (value.getTicker() != null) {
            gen.writeStringField("ticker", value.getTicker());
        } else {
            gen.writeNullField("ticker");
        }

        if (value.getReferenceDate() != null) {
            gen.writeStringField("referenceDate", DEFAULT_DATE_FORMATTER.format(value.getReferenceDate()));
        } else {
            gen.writeNullField("referenceDate");
        }

        gen.writeEndObject();
    }
}
