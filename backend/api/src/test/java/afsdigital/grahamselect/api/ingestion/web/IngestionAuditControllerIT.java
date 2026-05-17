package afsdigital.grahamselect.api.ingestion.web;

import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.entities.IngestionAudit;
import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.entities.IngestionAudit.IngestionStatus;
import afsdigital.grahamselect.common.ingestion.infrastructure.persistence.jpa.repositories.IngestionAuditRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.liquibase.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class IngestionAuditControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private IngestionAuditRepository repository;

    @Test
    public void shouldReturnIngestionHistoryForUser() throws Exception {
        String userId = "user-123";
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        repository.save(IngestionAudit.builder()
                .userId(userId)
                .correlationId("corr-1")
                .fileName("file1.xlsx")
                .uploadDate(now)
                .status(IngestionStatus.SUCESSO)
                .totalLines(100)
                .processedLines(100)
                .errorLines(0)
                .createdAt(now)
                .build());

        repository.save(IngestionAudit.builder()
                .userId(userId)
                .correlationId("corr-2")
                .fileName("file2.xlsx")
                .uploadDate(now.minusDays(1))
                .status(IngestionStatus.ERRO)
                .totalLines(50)
                .processedLines(0)
                .errorLines(50)
                .createdAt(now.minusDays(1))
                .build());

        mockMvc.perform(get("/api/v1/ingestion/history")
                .with(jwt().jwt(j -> j.subject(userId))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].fileName").value("file1.xlsx"))
                .andExpect(jsonPath("$[0].status").value("SUCESSO"))
                .andExpect(jsonPath("$[1].fileName").value("file2.xlsx"))
                .andExpect(jsonPath("$[1].status").value("ERRO"));
    }

    @Test
    public void shouldNotReturnHistoryFromOtherUsers() throws Exception {
        repository.save(IngestionAudit.builder()
                .userId("other-user")
                .correlationId("corr-other")
                .fileName("other.xlsx")
                .uploadDate(LocalDateTime.now(ZoneOffset.UTC))
                .status(IngestionStatus.SUCESSO)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build());

        mockMvc.perform(get("/api/v1/ingestion/history")
                .with(jwt().jwt(j -> j.subject("user-123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
