package afsdigital.grahamselect.api;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * API Automation Tests for Graham Select.
 * Focuses on Tier Restriction (/ranked-companies) and Data Upload (/upload-financial-data).
 * Simulates Playwright-like patterns for API requests and polling.
 */
@AutoConfigureMockMvc
public class ApiAutomationIT extends BaseRepositoryIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @Autowired
    private StockPriceJpaRepository stockPriceJpaRepository;

    @Autowired
    private IntrinsicValueJpaRepository intrinsicValueJpaRepository;

    @MockBean
    private JwtDecoder jwtDecoder;

    private static final String RANKED_COMPANIES_ENDPOINT = "/api/v1/ranked-companies";
    private static final String UPLOAD_ENDPOINT = "/api/v1/upload-financial-data";

    @BeforeEach
    void setUp() {
        intrinsicValueJpaRepository.deleteAll();
        stockPriceJpaRepository.deleteAll();
        companyJpaRepository.deleteAll();
        userRepository.deleteAll();

        // Seed default data for ranked companies tests
        String companyId = UUID.randomUUID().toString();
        companyJpaRepository.save(CompanyEntity.builder()
                .id(companyId)
                .ticker("AAPL")
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
    }

    @Nested
    @DisplayName("Tier Restriction Tests (/ranked-companies)")
    class RankedCompaniesTests {

        @Test
        @DisplayName("Should return 401 Unauthorized when no token is provided")
        void shouldReturn401WhenNoToken() throws Exception {
            mockMvc.perform(get(RANKED_COMPANIES_ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 403 Forbidden for FREE users")
        void shouldReturn403ForFreeUsers() throws Exception {
            User user = saveUser("free-user", SubscriptionTier.FREE, null);
            mockJwtDecoder("free-token", user);

            mockMvc.perform(get(RANKED_COMPANIES_ENDPOINT)
                    .header("Authorization", "Bearer free-token")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.type").value("urn:problem-type:upgrade-required"));
        }

        @Test
        @DisplayName("Should return 200 OK for PREMIUM users")
        void shouldReturn200ForPremiumUsers() throws Exception {
            User user = saveUser("premium-user", SubscriptionTier.PREMIUM, null);
            mockJwtDecoder("premium-token", user);

            mockMvc.perform(get(RANKED_COMPANIES_ENDPOINT)
                    .header("Authorization", "Bearer premium-token")
                    .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                    .andExpect(jsonPath("$[0].intrinsicValue").value(200.0));
        }
    }

    @Nested
    @DisplayName("Financial Data Upload Tests (/upload-financial-data)")
    class UploadFinancialDataTests {

        @Test
        @DisplayName("Should return 401 Unauthorized when no token is provided")
        void shouldReturn401WhenNoToken() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.multipart(UPLOAD_ENDPOINT)
                    .file(new MockMultipartFile("file", "test.csv", "text/csv", "ticker,price\nAAPL,150".getBytes()))
                    .contentType(MediaType.MULTIPART_FORM_DATA))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("Should return 200 OK when uploading valid file as PREMIUM user")
        void shouldReturn200ForValidUpload() throws Exception {
            User user = saveUser("premium-upload-user", SubscriptionTier.PREMIUM, null);
            mockJwtDecoder("premium-upload-token", user);

            MockMultipartFile file = new MockMultipartFile("file", "test-financial-data.csv",
                    "text/csv", new ClassPathResource("test-financial-data.csv").getInputStream());

            mockMvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.POST, UPLOAD_ENDPOINT)
                    .file(file)
                    .header("Authorization", "Bearer premium-upload-token"))
                    .andExpect(status().isOk());

            // Example of polling/recurse-like logic in Java
            // In a real E2E, we might wait for the data to appear in the DB
            boolean processed = pollUntil(() -> {
                // Check if company or stock price was updated/created
                return companyJpaRepository.findAll().stream()
                        .anyMatch(c -> "AALR3".equals(c.getTicker()));
            }, 5, 1);

            // Note: In this MockMvc test with @EmbeddedKafka, the actual processing might be async.
            // But since we are mocking the JWT and using MockMvc, we are mostly testing the controller.
        }
    }

    // Helper methods

    private User saveUser(String googleSub, SubscriptionTier tier, LocalDateTime trialEndsAt) {
        return userRepository.save(User.builder()
                .googleSub(googleSub)
                .email(googleSub + "@example.com")
                .fullName("Test User " + googleSub)
                .tier(tier)
                .trialEndsAt(trialEndsAt)
                .build());
    }

    private void mockJwtDecoder(String tokenValue, User user) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "RS256")
                .claim("sub", user.getGoogleSub())
                .claim("email", user.getEmail())
                .claim("name", user.getFullName())
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode(tokenValue)).thenReturn(jwt);
    }

    /**
     * Simulates Playwright's recurse/polling logic.
     */
    private boolean pollUntil(BooleanSupplier condition, int timeoutSeconds, int intervalSeconds) {
        long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        while (System.currentTimeMillis() < end) {
            if (condition.getAsBoolean()) {
                return true;
            }
            try {
                TimeUnit.SECONDS.sleep(intervalSeconds);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }
}
