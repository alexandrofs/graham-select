package afsdigital.grahamselect.common.domain.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialDataKeySerializationTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void shouldSerializeFinancialDataKey() throws Exception {
        FinancialDataKey key = new FinancialDataKey("AALR3", LocalDate.of(2026, 3, 6));

        String json = objectMapper.writeValueAsString(key);

        assertThat(json).isEqualTo("{\"ticker\":\"AALR3\",\"referenceDate\":\"2026-03-06\"}");
    }

    @Test
    void shouldDeserializeFinancialDataKey() throws Exception {
        String json = "{\"ticker\":\"AALR3\",\"referenceDate\":\"2026-03-06\"}";

        FinancialDataKey key = objectMapper.readValue(json, FinancialDataKey.class);

        assertThat(key.getTicker()).isEqualTo("AALR3");
        assertThat(key.getReferenceDate()).isEqualTo(LocalDate.of(2026, 3, 6));
    }
}
