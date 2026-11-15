package afsdigital.grahamselect.api.upload.web;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import afsdigital.grahamselect.api.auth.infrastructure.security.PremiumFeatureAccessGuard;
import afsdigital.grahamselect.api.auth.infrastructure.security.PremiumFeatureAccessDeniedException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.liquibase.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:testdb_brokerage;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@EmbeddedKafka(topics = TopicConstants.B3_UPLOAD_TOPIC, partitions = 1)
public class BrokerageNoteUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private DefaultKafkaConsumerFactory<String, String> defaultKafkaConsumerFactory;

    @MockBean
    private PremiumFeatureAccessGuard premiumFeatureAccessGuard;

    private byte[] createPdf(List<String> lines) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);
                cs.newLineAtOffset(40, 750);
                for (String line : lines) {
                    cs.showText(line);
                    cs.newLineAtOffset(0, -15);
                }
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("Deve retornar 400 quando o arquivo não for PDF")
    void shouldReturn400WhenExtensionIsNotPdf() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "trades.txt", MediaType.TEXT_PLAIN_VALUE, "content".getBytes());

        mockMvc.perform(multipart("/api/v1/upload/brokerage-note")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Apenas arquivos .pdf de notas de corretagem (padrão SINACOR) são suportados neste endpoint."));
    }

    @Test
    @DisplayName("Deve retornar 400 quando o arquivo estiver vazio")
    void shouldReturn400WhenFileIsEmpty() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", MediaType.APPLICATION_PDF_VALUE, new byte[0]);

        mockMvc.perform(multipart("/api/v1/upload/brokerage-note")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar 400 quando o PDF não for uma nota SINACOR")
    void shouldReturn400WhenPdfIsNotSinacor() throws Exception {
        byte[] content = createPdf(List.of("RELATÓRIO FINANCEIRO", "Saldo: 100"));
        MockMultipartFile file = new MockMultipartFile("file", "relatorio.pdf", MediaType.APPLICATION_PDF_VALUE, content);

        mockMvc.perform(multipart("/api/v1/upload/brokerage-note")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Arquivo PDF inválido: Não foi possível identificar o padrão de Nota de Corretagem SINACOR."));
    }

    @Test
    @DisplayName("Deve retornar 202 quando o PDF for uma nota SINACOR válida e publicar evento no Kafka")
    void shouldReturn202WhenSinacorPdfIsValidAndEmitEvent() throws Exception {
        byte[] content = createPdf(List.of(
                "NOTA DE CORRETAGEM",
                "XP INVESTIMENTOS CCTVM S/A",
                "Nr. nota: 12345 Data pregão: 10/05/2026 Folha: 1",
                "1-BOVESPA C VISTA PETR4 PETROBRAS PN 100 38,50 3.850,00 D"
        ));
        MockMultipartFile file = new MockMultipartFile("file", "nota_xp.pdf", MediaType.APPLICATION_PDF_VALUE, content);

        mockMvc.perform(multipart("/api/v1/upload/brokerage-note")
                        .file(file)
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.correlationId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("RECEIVED"));

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        kafkaTemplate.setConsumerFactory(defaultKafkaConsumerFactory);
        ConsumerRecord<String, String> consumerRecord = kafkaTemplate.receive(TopicConstants.B3_UPLOAD_TOPIC, 0, 0,
                Duration.ofSeconds(5));

        assertThat(consumerRecord).isNotNull();
        assertThat(consumerRecord.key().replace("\"", "")).isEqualTo("user-123");

        FileUploadedEvent event = objectMapper.readValue(consumerRecord.value(), FileUploadedEvent.class);
        assertThat(event.fileName()).isEqualTo("nota_xp.pdf");
        assertThat(event.userId()).isEqualTo("user-123");
        assertThat(event.correlationId()).isNotNull();
        assertThat(event.storagePath()).isNotNull();
        assertThat(event.eventId()).isNotNull();
    }

    @Test
    @DisplayName("Deve retornar 403 quando profileId for informado por usuário sem plano Premium")
    void shouldReturn403WhenProfileIdProvidedByNonPremiumUser() throws Exception {
        when(premiumFeatureAccessGuard.canAccessPremiumFeatures())
                .thenThrow(new PremiumFeatureAccessDeniedException("This feature requires a Premium subscription."));

        byte[] content = createPdf(List.of(
                "NOTA DE CORRETAGEM",
                "XP INVESTIMENTOS CCTVM S/A",
                "Nr. nota: 12345 Data pregão: 10/05/2026 Folha: 1",
                "1-BOVESPA C VISTA PETR4 PETROBRAS PN 100 38,50 3.850,00 D"
        ));
        MockMultipartFile file = new MockMultipartFile("file", "nota_xp.pdf", MediaType.APPLICATION_PDF_VALUE, content);

        mockMvc.perform(multipart("/api/v1/upload/brokerage-note")
                        .file(file)
                        .param("profileId", java.util.UUID.randomUUID().toString())
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar 404 quando profileId tiver formato de UUID inválido")
    void shouldReturn404WhenProfileIdHasInvalidFormat() throws Exception {
        when(premiumFeatureAccessGuard.canAccessPremiumFeatures()).thenReturn(true);

        byte[] content = createPdf(List.of(
                "NOTA DE CORRETAGEM",
                "XP INVESTIMENTOS CCTVM S/A",
                "Nr. nota: 12345 Data pregão: 10/05/2026 Folha: 1",
                "1-BOVESPA C VISTA PETR4 PETROBRAS PN 100 38,50 3.850,00 D"
        ));
        MockMultipartFile file = new MockMultipartFile("file", "nota_xp.pdf", MediaType.APPLICATION_PDF_VALUE, content);

        mockMvc.perform(multipart("/api/v1/upload/brokerage-note")
                        .file(file)
                        .param("profileId", "invalid-uuid-format")
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Deve retornar 404 quando profileId não existir ou não pertencer ao usuário")
    void shouldReturn404WhenProfileIdDoesNotExistOrNotOwned() throws Exception {
        when(premiumFeatureAccessGuard.canAccessPremiumFeatures()).thenReturn(true);

        byte[] content = createPdf(List.of(
                "NOTA DE CORRETAGEM",
                "XP INVESTIMENTOS CCTVM S/A",
                "Nr. nota: 12345 Data pregão: 10/05/2026 Folha: 1",
                "1-BOVESPA C VISTA PETR4 PETROBRAS PN 100 38,50 3.850,00 D"
        ));
        MockMultipartFile file = new MockMultipartFile("file", "nota_xp.pdf", MediaType.APPLICATION_PDF_VALUE, content);

        mockMvc.perform(multipart("/api/v1/upload/brokerage-note")
                        .file(file)
                        .param("profileId", java.util.UUID.randomUUID().toString())
                        .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isNotFound());
    }
}
