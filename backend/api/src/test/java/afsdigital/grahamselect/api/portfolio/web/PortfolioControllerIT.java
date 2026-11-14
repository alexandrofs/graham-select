package afsdigital.grahamselect.api.portfolio.web;

import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.entities.TradeEntity;
import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaTradeRepository;
import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.entities.AssetTagEntity;
import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.entities.PortfolioTagEntity;
import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.repositories.JpaAssetTagRepository;
import afsdigital.grahamselect.api.tags.infrastructure.persistence.jpa.repositories.JpaPortfolioTagRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.liquibase.enabled=true",
    "spring.datasource.url=jdbc:h2:mem:testdb_custody;DB_CLOSE_DELAY=-1;MODE=MySQL",
    "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class PortfolioControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaTradeRepository tradeRepository;

    @Autowired
    private JpaPortfolioTagRepository portfolioTagRepository;

    @Autowired
    private JpaAssetTagRepository assetTagRepository;

    private static final String USER_ID = "user-custody-it";

    @BeforeEach
    void setUp() {
        assetTagRepository.deleteAll();
        portfolioTagRepository.deleteAll();
        tradeRepository.deleteAll();
    }

    @Test
    void shouldReturnCustodyPositionsWithCorrectStructure() throws Exception {
        // Dado: usuário com 1 trade de COMPRA
        tradeRepository.save(TradeEntity.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .ticker("PETR4")
                .side("COMPRA")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("30.00"))
                .tradeDate(LocalDate.now())
                .broker("XP")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build());

        mockMvc.perform(get("/api/v1/portfolios/custody")
                .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                // Estrutura obrigatória: { "data": [...], "meta": { "total": N } }
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.meta.total").value(1))
                // Campos obrigatórios na posição
                .andExpect(jsonPath("$.data[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.data[0].quantity").exists())
                .andExpect(jsonPath("$.data[0].averagePrice").exists())
                .andExpect(jsonPath("$.data[0].currentPrice").exists())
                .andExpect(jsonPath("$.data[0].marketValue").exists())
                .andExpect(jsonPath("$.data[0].gainLossPercentage").exists())
                .andExpect(jsonPath("$.data[0].priceSource").exists());
    }

    @Test
    void shouldReturnEmptyDataWhenUserHasNoTrades() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/custody")
                .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    void shouldNotReturnPositionsFromOtherUsers() throws Exception {
        // Dado: trade pertencente a outro usuário
        tradeRepository.save(TradeEntity.builder()
                .id(UUID.randomUUID())
                .userId("other-user-id")
                .ticker("VALE3")
                .side("COMPRA")
                .quantity(new BigDecimal("5"))
                .price(new BigDecimal("80.00"))
                .tradeDate(LocalDate.now())
                .broker("BTG")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build());

        // Quando: usuário diferente consulta a custódia
        mockMvc.perform(get("/api/v1/portfolios/custody")
                .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    void shouldExcludeZeroedPositionsFromCustody() throws Exception {
        // Dado: COMPRA e VENDA completa — posição zerada
        tradeRepository.save(TradeEntity.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .ticker("ITUB4")
                .side("COMPRA")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("28.50"))
                .tradeDate(LocalDate.now().minusDays(2))
                .broker("XP")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build());

        tradeRepository.save(TradeEntity.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .ticker("ITUB4")
                .side("VENDA")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("32.00"))
                .tradeDate(LocalDate.now().minusDays(1))
                .broker("XP")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build());

        // Então: posição zerada não deve aparecer
        mockMvc.perform(get("/api/v1/portfolios/custody")
                .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    void shouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/portfolios/custody"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldFilterSummaryCustodyAndEvolutionByTagAndReportGlobalUntaggedMeta() throws Exception {
        LocalDate utcToday = LocalDate.now(ZoneOffset.UTC);
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        tradeRepository.save(TradeEntity.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID).ticker("PETR4").side("COMPRA")
                .quantity(new BigDecimal("10")).price(new BigDecimal("30.00"))
                .tradeDate(utcToday).broker("XP").createdAt(nowUtc).build());
        tradeRepository.save(TradeEntity.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID).ticker("PETR4").side("DIVIDENDO")
                .quantity(new BigDecimal("10")).price(new BigDecimal("1.50"))
                .tradeDate(utcToday).broker("XP").createdAt(nowUtc).build());
        tradeRepository.save(TradeEntity.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID).ticker("VALE3").side("COMPRA")
                .quantity(new BigDecimal("5")).price(new BigDecimal("60.00"))
                .tradeDate(utcToday).broker("BTG").createdAt(nowUtc).build());

        UUID tagId = UUID.randomUUID();
        portfolioTagRepository.save(PortfolioTagEntity.builder()
                .id(tagId).userId(USER_ID).name("Dividendos").normalizedName("dividendos")
                .color("#10B981").createdAt(LocalDateTime.now(ZoneOffset.UTC)).isNew(true).build());
        assetTagRepository.save(AssetTagEntity.builder()
                .id(UUID.randomUUID()).userId(USER_ID).tagId(tagId).ticker("PETR4")
                .createdAt(LocalDateTime.now(ZoneOffset.UTC)).isNew(true).build());

        mockMvc.perform(get("/api/v1/portfolios/summary")
                        .param("tagId", tagId.toString())
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accumulatedDividends").value(15.0));

        mockMvc.perform(get("/api/v1/portfolios/custody")
                        .param("tagId", tagId.toString())
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.meta.total").value(1))
                .andExpect(jsonPath("$.meta.untaggedAssetsCount").value(1))
                .andExpect(jsonPath("$.meta.untaggedTickers", containsInAnyOrder("VALE3")));

        mockMvc.perform(get("/api/v1/portfolios/evolution")
                        .param("tagId", tagId.toString())
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyData", hasSize(12)))
                .andExpect(jsonPath("$.monthlyData[11].totalContributions").value(300.0))
                .andExpect(jsonPath("$.monthlyData[11].totalDividends").value(15.0));
    }

    @Test
    void shouldReturn404WhenPortfolioFilteredByForeignOrNonExistentTag() throws Exception {
        UUID foreignTagId = UUID.randomUUID();
        portfolioTagRepository.save(PortfolioTagEntity.builder()
                .id(foreignTagId).userId("another-user").name("Outra").normalizedName("outra")
                .color("#6366F1").createdAt(LocalDateTime.now(ZoneOffset.UTC)).isNew(true).build());

        mockMvc.perform(get("/api/v1/portfolios/summary")
                        .param("tagId", foreignTagId.toString())
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/portfolios/custody")
                        .param("tagId", foreignTagId.toString())
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/portfolios/evolution")
                        .param("tagId", foreignTagId.toString())
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isNotFound());
    }
}
