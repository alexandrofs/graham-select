package afsdigital.grahamselect.api.portfolio.web;

import afsdigital.grahamselect.common.portfolio.infrastructure.persistence.jpa.entities.TradeEntity;
import afsdigital.grahamselect.common.portfolio.infrastructure.persistence.jpa.repositories.TradeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private TradeRepository tradeRepository;

    private static final String USER_ID = "user-custody-it";

    @BeforeEach
    void setUp() {
        tradeRepository.deleteAll();
    }

    @Test
    void shouldReturnCustodyPositionsWithCorrectStructure() throws Exception {
        // Dado: usuário com 1 trade de COMPRA
        tradeRepository.save(TradeEntity.builder()
                .userId(USER_ID)
                .ticker("PETR4")
                .side("COMPRA")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("30.00"))
                .tradeDate(LocalDate.now())
                .broker("XP")
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
                .userId("other-user-id")
                .ticker("VALE3")
                .side("COMPRA")
                .quantity(new BigDecimal("5"))
                .price(new BigDecimal("80.00"))
                .tradeDate(LocalDate.now())
                .broker("BTG")
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
                .userId(USER_ID)
                .ticker("ITUB4")
                .side("COMPRA")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("28.50"))
                .tradeDate(LocalDate.now().minusDays(2))
                .broker("XP")
                .build());

        tradeRepository.save(TradeEntity.builder()
                .userId(USER_ID)
                .ticker("ITUB4")
                .side("VENDA")
                .quantity(new BigDecimal("10"))
                .price(new BigDecimal("32.00"))
                .tradeDate(LocalDate.now().minusDays(1))
                .broker("XP")
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
}
