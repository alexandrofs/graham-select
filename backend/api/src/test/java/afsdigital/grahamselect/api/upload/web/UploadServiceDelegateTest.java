package afsdigital.grahamselect.api.upload.web;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.domain.entities.FinancialDataEvent;
import afsdigital.grahamselect.common.domain.entities.FinancialDataKey;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.time.Duration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

@EmbeddedKafka(topics = TopicConstants.FINANCIAL_DATA_TOPIC, partitions = 1, bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@AutoConfigureMockMvc
public class UploadServiceDelegateTest extends BaseRepositoryIT {

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    DefaultKafkaConsumerFactory<String, String> defaultKafkaConsumerFactory;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    private String testUserId;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User testUser = User.builder()
                .googleSub("some-google-sub")
                .email("test@example.com")
                .fullName("Test User")
                .tier(SubscriptionTier.PREMIUM)
                .build();
        testUser = userRepository.save(testUser);
        testUserId = testUser.getId().toString();
    }

    @Test
    public void success() throws Exception {

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        MockMultipartFile multipartFile = new MockMultipartFile("file",
                new ClassPathResource("test-financial-data.csv").getInputStream());

        mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.POST, "/api/v1/upload-financial-data")
                .file(multipartFile)
                .with(jwt().jwt(j -> j.subject(testUserId))))
                .andExpect(MockMvcResultMatchers.status().isOk());

        kafkaTemplate.setConsumerFactory(defaultKafkaConsumerFactory);
        ConsumerRecord<String, String> consumerRecord = kafkaTemplate.receive(TopicConstants.FINANCIAL_DATA_TOPIC, 0, 0,
                Duration.ofSeconds(1));
        assert consumerRecord != null;
        FinancialDataKey financialDataKey = objectMapper.readValue(consumerRecord.key(), FinancialDataKey.class);
        FinancialDataEvent financialDataEvent = objectMapper.readValue(consumerRecord.value(),
                FinancialDataEvent.class);
        Assertions.assertThat(financialDataKey.getTicker()).isEqualTo("AALR3");
        Assertions.assertThat(financialDataEvent.getTicker()).isEqualTo("AALR3");

    }
}
