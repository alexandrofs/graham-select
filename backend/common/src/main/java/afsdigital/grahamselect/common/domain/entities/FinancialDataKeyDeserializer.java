package afsdigital.grahamselect.common.domain.entities;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.time.LocalDate;

public class FinancialDataKeyDeserializer extends JsonDeserializer<FinancialDataKey> {

    @Override
    public FinancialDataKey deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        ObjectCodec codec = p.getCodec();
        JsonNode node = codec.readTree(p);

        String ticker = node.has("ticker") && !node.get("ticker").isNull()
                ? node.get("ticker").asText()
                : null;

        LocalDate referenceDate = null;
        if (node.has("referenceDate") && !node.get("referenceDate").isNull()) {
            referenceDate = LocalDate.parse(node.get("referenceDate").asText());
        }

        return new FinancialDataKey(ticker, referenceDate);
    }
}
