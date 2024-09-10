package afsdigital.grahamselect.api.upload.web;

import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.common.domain.entities.FinancialDataKey;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.io.IOException;
import java.time.Duration;

@SpringBootTest
@AutoConfigureMockMvc
@EmbeddedKafka(topics = TopicConstants.FINANCIAL_DATA_TOPIC, partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
public class UploadServiceDelegateTest {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    DefaultKafkaConsumerFactory<String, String> defaultKafkaConsumerFactory;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void success() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        MockMultipartFile multipartFile = new MockMultipartFile("file", new ClassPathResource("test-financial-data.csv").getInputStream());

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.POST, "/api/v1/upload-financial-data")
                .file(multipartFile)).andExpect(MockMvcResultMatchers.status().is(200));

        kafkaTemplate.setConsumerFactory(defaultKafkaConsumerFactory);
        ConsumerRecord<String, String> consumerRecord = kafkaTemplate.receive(TopicConstants.FINANCIAL_DATA_TOPIC, 0, 0, Duration.ofSeconds(1));
        assert consumerRecord != null;
        FinancialDataKey financialDataKey = objectMapper.readValue(consumerRecord.key(), FinancialDataKey.class);
        FinancialDataEvent financialDataEvent = objectMapper.readValue(consumerRecord.value(), FinancialDataEvent.class);
        Assertions.assertThat(financialDataKey.getTicker()).isEqualTo("AALR3");
        Assertions.assertThat(financialDataEvent.getTicker()).isEqualTo("AALR3");

    }
}
