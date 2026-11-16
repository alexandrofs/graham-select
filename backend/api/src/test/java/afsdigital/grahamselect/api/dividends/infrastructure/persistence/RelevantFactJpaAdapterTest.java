package afsdigital.grahamselect.api.dividends.infrastructure.persistence;

import afsdigital.grahamselect.api.dividends.infrastructure.persistence.jpa.entities.RelevantFactEntity;
import afsdigital.grahamselect.api.dividends.infrastructure.persistence.jpa.repositories.RelevantFactJpaRepository;
import afsdigital.grahamselect.common.dividends.domain.entities.RelevantFact;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RelevantFactJpaAdapterTest {

    @Mock
    private RelevantFactJpaRepository repository;

    @InjectMocks
    private RelevantFactJpaAdapter adapter;

    @Test
    @DisplayName("Deve salvar novo fato relevante quando não existir duplicata")
    void shouldSaveNewRelevantFact() {
        Instant now = Instant.now();
        RelevantFact fact = RelevantFact.builder()
                .ticker("VALE3")
                .title("Aprovação de Proventos Trimestrais")
                .summary("Conselho aprovou proventos")
                .sourceUrl("https://cvm.gov.br/doc/123")
                .category("FATO_RELEVANTE")
                .publicationDate(now)
                .build();

        when(repository.existsByTickerAndPublicationDate(eq("VALE3"), any(LocalDateTime.class)))
                .thenReturn(false);

        when(repository.save(any(RelevantFactEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        RelevantFact saved = adapter.save(fact);

        assertThat(saved).isNotNull();
        assertThat(saved.getTicker()).isEqualTo("VALE3");
        assertThat(saved.getTitle()).isEqualTo("Aprovação de Proventos Trimestrais");
        verify(repository).save(any(RelevantFactEntity.class));
    }

    @Test
    @DisplayName("Deve ignorar inserção de fato relevante duplicado para mesmo ticker e data")
    void shouldIgnoreDuplicateRelevantFact() {
        Instant now = Instant.now();
        RelevantFact fact = RelevantFact.builder()
                .ticker("VALE3")
                .title("Aprovação de Proventos Trimestrais")
                .publicationDate(now)
                .build();

        when(repository.existsByTickerAndPublicationDate(eq("VALE3"), any(LocalDateTime.class)))
                .thenReturn(true);

        RelevantFact result = adapter.save(fact);

        assertThat(result).isNotNull();
        verify(repository, never()).save(any(RelevantFactEntity.class));
    }

    @Test
    @DisplayName("Deve buscar fatos relevantes recentes convertendo entidades para domínio")
    void shouldFindRecentFacts() {
        Instant now = Instant.now();
        LocalDateTime localNow = LocalDateTime.ofInstant(now, ZoneOffset.UTC);
        UUID id = UUID.randomUUID();

        RelevantFactEntity entity = RelevantFactEntity.builder()
                .id(id)
                .ticker("VALE3")
                .title("Fato Relevante")
                .summary("Resumo")
                .sourceUrl("https://cvm.gov.br")
                .category("FATO_RELEVANTE")
                .publicationDate(localNow)
                .createdAt(localNow)
                .build();

        when(repository.findRecentFacts(eq(List.of("VALE3")), eq(false), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(entity));

        List<RelevantFact> results = adapter.findByTickersAndDateRange(List.of("VALE3"), null, null, 10);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo(id);
        assertThat(results.get(0).getTicker()).isEqualTo("VALE3");
    }
}
