package afsdigital.grahamselect.api.portfolio.web;

import afsdigital.grahamselect.api.family.infrastructure.persistence.jpa.entities.FamilyProfileEntity;
import afsdigital.grahamselect.api.family.infrastructure.persistence.jpa.repositories.JpaFamilyProfileRepository;
import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaManualTradeAuditRepository;
import afsdigital.grahamselect.api.portfolio.infrastructure.persistence.jpa.repositories.JpaTradeRepository;
import afsdigital.grahamselect.common.portfolio.application.repository.ValuationEventPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.liquibase.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:testdb_manual_crud;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.jpa.hibernate.ddl-auto=none"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class TradeManualControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JpaTradeRepository tradeRepository;

    @Autowired
    private JpaManualTradeAuditRepository auditRepository;

    @Autowired
    private JpaFamilyProfileRepository familyProfileRepository;

    @MockitoBean
    private ValuationEventPort valuationEventPort;

    private static final String USER_ID = "user-manual-it";
    private static final String OTHER_USER_ID = "other-user-manual-it";

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
        tradeRepository.deleteAll();
        familyProfileRepository.deleteAll();
    }

    @Test
    void shouldCreateListUpdateAndDeleteManualTradeWithAssetClass() throws Exception {
        String createPayload = """
                {
                  "ticker": "IVVB11",
                  "side": "COMPRA",
                  "tradeDate": "2026-05-10",
                  "quantity": 10,
                  "price": 310.50,
                  "broker": "Manual",
                  "assetClass": "Internacional"
                }
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/trades/manual")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("IVVB11"))
                .andExpect(jsonPath("$.assetClass").value("Internacional"))
                .andReturn();

        String tradeId = com.jayway.jsonpath.JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        // Duplicate POST returns 409
        mockMvc.perform(post("/api/v1/trades/manual")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPayload))
                .andExpect(status().isConflict());

        // GET lists the trade
        mockMvc.perform(get("/api/v1/trades/manual")
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(tradeId))
                .andExpect(jsonPath("$[0].assetClass").value("Internacional"));

        // PUT updates the trade
        String updatePayload = """
                {
                  "ticker": "IVVB11",
                  "side": "COMPRA",
                  "tradeDate": "2026-05-11",
                  "quantity": 15,
                  "price": 315.00,
                  "broker": "XP",
                  "assetClass": "Internacional"
                }
                """;

        mockMvc.perform(put("/api/v1/trades/manual/{id}", tradeId)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(15))
                .andExpect(jsonPath("$.broker").value("XP"));

        // Other user cannot update or delete -> 404
        mockMvc.perform(delete("/api/v1/trades/manual/{id}", tradeId)
                        .with(jwt().jwt(j -> j.subject(OTHER_USER_ID))))
                .andExpect(status().isNotFound());

        // Owner deletes -> 204
        mockMvc.perform(delete("/api/v1/trades/manual/{id}", tradeId)
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isNoContent());

        // List is now empty
        mockMvc.perform(get("/api/v1/trades/manual")
                        .with(jwt().jwt(j -> j.subject(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void shouldReturn404WhenProfileIdDoesNotBelongToUserAndAllowValidProfileId() throws Exception {
        UUID ownedProfileId = UUID.randomUUID();
        familyProfileRepository.save(FamilyProfileEntity.builder()
                .id(ownedProfileId)
                .masterUserId(USER_ID)
                .cpfDigits("12345678901")
                .name("Filho")
                .relationship("FILHO")
                .active(true)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build());

        String foreignProfilePayload = """
                {
                  "ticker": "BTC",
                  "tradeDate": "2026-05-10",
                  "quantity": 0.05,
                  "price": 350000.00,
                  "assetClass": "Criptoativos",
                  "profileId": "%s"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(post("/api/v1/trades/manual")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(foreignProfilePayload))
                .andExpect(status().isNotFound());

        String ownedProfilePayload = """
                {
                  "ticker": "BTC",
                  "tradeDate": "2026-05-10",
                  "quantity": 0.05,
                  "price": 350000.00,
                  "assetClass": "Criptoativos",
                  "profileId": "%s"
                }
                """.formatted(ownedProfileId);

        mockMvc.perform(post("/api/v1/trades/manual")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownedProfilePayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profileId").value(ownedProfileId.toString()))
                .andExpect(jsonPath("$.side").value("COMPRA"))
                .andExpect(jsonPath("$.broker").value("Manual"));
    }

    @Test
    void shouldReturn409ConflictOnPutWhenSignatureMatchesAnotherTrade() throws Exception {
        // 1. Sem profileId (profileId = null)
        String firstNullProfile = """
                {
                  "ticker": "PETR4",
                  "side": "COMPRA",
                  "tradeDate": "2026-05-10",
                  "quantity": 10,
                  "price": 30.00,
                  "broker": "XP"
                }
                """;
        String secondNullProfile = """
                {
                  "ticker": "VALE3",
                  "side": "COMPRA",
                  "tradeDate": "2026-05-10",
                  "quantity": 10,
                  "price": 60.00,
                  "broker": "XP"
                }
                """;

        mockMvc.perform(post("/api/v1/trades/manual")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstNullProfile))
                .andExpect(status().isCreated());

        MvcResult secondRes = mockMvc.perform(post("/api/v1/trades/manual")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondNullProfile))
                .andExpect(status().isCreated())
                .andReturn();
        String secondId = com.jayway.jsonpath.JsonPath.read(secondRes.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/v1/trades/manual/{id}", secondId)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstNullProfile))
                .andExpect(status().isConflict());

        // 2. Com profileId não nulo
        UUID profileId = UUID.randomUUID();
        familyProfileRepository.save(FamilyProfileEntity.builder()
                .id(profileId)
                .masterUserId(USER_ID)
                .cpfDigits("99988877766")
                .name("Cônjuge")
                .relationship("CONJUGE")
                .active(true)
                .createdAt(LocalDateTime.now(ZoneOffset.UTC))
                .build());

        String firstWithProfile = """
                {
                  "ticker": "ITUB4",
                  "side": "COMPRA",
                  "tradeDate": "2026-05-12",
                  "quantity": 5,
                  "price": 35.00,
                  "broker": "Clear",
                  "profileId": "%s"
                }
                """.formatted(profileId);
        String secondWithProfile = """
                {
                  "ticker": "BBAS3",
                  "side": "COMPRA",
                  "tradeDate": "2026-05-12",
                  "quantity": 5,
                  "price": 28.00,
                  "broker": "Clear",
                  "profileId": "%s"
                }
                """.formatted(profileId);

        mockMvc.perform(post("/api/v1/trades/manual")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstWithProfile))
                .andExpect(status().isCreated());

        MvcResult secondProfileRes = mockMvc.perform(post("/api/v1/trades/manual")
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(secondWithProfile))
                .andExpect(status().isCreated())
                .andReturn();
        String secondProfileId = com.jayway.jsonpath.JsonPath.read(secondProfileRes.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/v1/trades/manual/{id}", secondProfileId)
                        .with(jwt().jwt(j -> j.subject(USER_ID)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(firstWithProfile))
                .andExpect(status().isConflict());
    }
}
