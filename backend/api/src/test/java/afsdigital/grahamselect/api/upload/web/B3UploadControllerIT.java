package afsdigital.grahamselect.api.upload.web;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
    "spring.liquibase.enabled=false",
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.jpa.hibernate.ddl-auto=none",
    "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(topics = TopicConstants.B3_UPLOAD_TOPIC, partitions = 1)
public class B3UploadControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private DefaultKafkaConsumerFactory<String, String> defaultKafkaConsumerFactory;

    @Test
    public void shouldReturn400WhenExtensionIsInvalid() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/api/v1/upload/b3")
                .file(file)
                .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn400WhenHeadersAreMissing() throws Exception {
        byte[] content = createExcel(new String[]{"Col1", "Col2"});
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        mockMvc.perform(multipart("/api/v1/upload/b3")
                .file(file)
                .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void shouldReturn202WhenFileIsValidAndEmitEvent() throws Exception {
        byte[] content = createExcel(new String[]{"Ticker", "Data", "Quantidade", "Preço"});
        MockMultipartFile file = new MockMultipartFile("file", "test.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", content);

        mockMvc.perform(multipart("/api/v1/upload/b3")
                .file(file)
                .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isAccepted());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        kafkaTemplate.setConsumerFactory(defaultKafkaConsumerFactory);
        ConsumerRecord<String, String> consumerRecord = kafkaTemplate.receive(TopicConstants.B3_UPLOAD_TOPIC, 0, 0,
                Duration.ofSeconds(5));

        assertThat(consumerRecord).isNotNull();
        assertThat(consumerRecord.key()).isEqualTo("user-123");

        FileUploadedEvent event = objectMapper.readValue(consumerRecord.value(), FileUploadedEvent.class);
        assertThat(event.fileName()).isEqualTo("test.xlsx");
        assertThat(event.userId()).isEqualTo("user-123");
        assertThat(event.correlationId()).isNotNull();
        assertThat(event.storagePath()).isNotNull();
    }

    private byte[] createExcel(String[] headers) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (Workbook wb = new Workbook(os, "Test", "1.0")) {
            Worksheet ws = wb.newWorksheet("Sheet 1");
            for (int i = 0; i < headers.length; i++) {
                ws.value(0, i, headers[i]);
            }
        }
        return os.toByteArray();
    }
}
