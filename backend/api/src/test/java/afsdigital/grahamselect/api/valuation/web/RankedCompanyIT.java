package afsdigital.grahamselect.api.valuation.web;

import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.StockPriceEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.IntrinsicValueJpaRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.StockPriceJpaRepository;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class RankedCompanyIT extends BaseRepositoryIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @Autowired
    private StockPriceJpaRepository stockPriceJpaRepository;

    @Autowired
    private IntrinsicValueJpaRepository intrinsicValueJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        intrinsicValueJpaRepository.deleteAll();
        stockPriceJpaRepository.deleteAll();
        companyJpaRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturnRankedCompanies() throws Exception {
        // Arrange
        String companyId = "AAPL";
        companyJpaRepository.save(CompanyEntity.builder()
                .id(companyId)
                .name("Apple Inc.")
                .build());

        stockPriceJpaRepository.save(StockPriceEntity.builder()
                .id(UUID.randomUUID().toString())
                .companyId(companyId)
                .priceDate(LocalDate.now())
                .price(BigDecimal.valueOf(150.0))
                .build());

        intrinsicValueJpaRepository.save(IntrinsicValueEntity.builder()
                .id(UUID.randomUUID().toString())
                .companyId(companyId)
                .calculationDate(LocalDate.now())
                .intrinsicValue(BigDecimal.valueOf(200.0))
                .build());

        User premiumUser = userRepository.save(User.builder()
                .googleSub("premium-ranked-sub")
                .email("premium-ranked@example.com")
                .fullName("Premium Ranked User")
                .tier(SubscriptionTier.PREMIUM)
                .trialEndsAt(LocalDateTime.now().minusDays(1))
                .build());

        Jwt jwt = Jwt.withTokenValue("ranked-premium-token")
                .header("alg", "RS256")
                .claim("sub", premiumUser.getGoogleSub())
                .claim("email", premiumUser.getEmail())
                .claim("name", premiumUser.getFullName())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode("ranked-premium-token")).thenReturn(jwt);

        // Act & Assert
        mockMvc.perform(get("/api/v1/ranked-companies")
                .header("Authorization", "Bearer ranked-premium-token")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].name").value("Apple Inc."))
                .andExpect(jsonPath("$[0].intrinsicValue").value(200.0))
                .andExpect(jsonPath("$[0].currentPrice").value(150.0))
                .andExpect(jsonPath("$[0].marginOfSafety").value(0.333333));
    }
}
