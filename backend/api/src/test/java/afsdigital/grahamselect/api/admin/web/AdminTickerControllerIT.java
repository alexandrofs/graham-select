package afsdigital.grahamselect.api.admin.web;

import afsdigital.grahamselect.api.admin.dto.CreateAdminTickerRequest;
import afsdigital.grahamselect.api.admin.dto.UpdateAdminTickerRequest;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.entities.CompanyEntity;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.jpa.repository.CompanyJpaRepository;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.domain.entities.UserRole;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc
class AdminTickerControllerIT extends BaseRepositoryIT {

    private static final String TICKERS_BASE_URL = "/api/v1/admin/tickers";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CompanyJpaRepository companyJpaRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        companyJpaRepository.deleteAll();
        userRepository.deleteAll();

        Jwt adminJwt = Jwt.withTokenValue("admin-jwt")
                .header("alg", "RS256")
                .claim("sub", "admin-sub")
                .claim("email", "admin@grahamselect.com")
                .claim("role", "ADMIN")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode("admin-jwt")).thenReturn(adminJwt);

        Jwt userJwt = Jwt.withTokenValue("user-jwt")
                .header("alg", "RS256")
                .claim("sub", "user-sub")
                .claim("email", "user@grahamselect.com")
                .claim("role", "USER")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        when(jwtDecoder.decode("user-jwt")).thenReturn(userJwt);
    }

    @Test
    @DisplayName("Should return 401 Unauthorized when request has no token")
    void shouldReturn401WhenNoToken() throws Exception {
        mockMvc.perform(get(TICKERS_BASE_URL)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Should return 403 Forbidden when user is not admin")
    void shouldReturn403WhenNotAdmin() throws Exception {
        mockMvc.perform(get(TICKERS_BASE_URL)
                        .header("Authorization", "Bearer user-jwt")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 200 with list of tickers for admin user")
    void shouldReturn200WithTickersForAdmin() throws Exception {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        companyJpaRepository.save(CompanyEntity.builder()
                .id(UUID.randomUUID().toString())
                .ticker("PETR4")
                .name("Petrobras")
                .assetClass("Ações")
                .sector("Petróleo")
                .createdAt(now)
                .updatedAt(now)
                .build());

        companyJpaRepository.save(CompanyEntity.builder()
                .id(UUID.randomUUID().toString())
                .ticker("VALE3")
                .name("Vale")
                .assetClass("Ações")
                .sector("Mineração")
                .createdAt(now)
                .updatedAt(now)
                .build());

        mockMvc.perform(get(TICKERS_BASE_URL)
                        .header("Authorization", "Bearer admin-jwt")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.data[1].ticker").value("VALE3"));
    }

    @Test
    @DisplayName("Should filter tickers by query and assetClass")
    void shouldFilterTickers() throws Exception {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        companyJpaRepository.save(CompanyEntity.builder()
                .id(UUID.randomUUID().toString())
                .ticker("HGLG11")
                .name("CSHG Logística")
                .assetClass("FIIs")
                .createdAt(now)
                .updatedAt(now)
                .build());

        companyJpaRepository.save(CompanyEntity.builder()
                .id(UUID.randomUUID().toString())
                .ticker("BOVA11")
                .name("iShares Ibovespa")
                .assetClass("ETFs")
                .createdAt(now)
                .updatedAt(now)
                .build());

        mockMvc.perform(get(TICKERS_BASE_URL)
                        .param("query", "BOVA")
                        .param("assetClass", "ETFs")
                        .header("Authorization", "Bearer admin-jwt")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].ticker").value("BOVA11"))
                .andExpect(jsonPath("$.data[0].assetClass").value("ETFs"));
    }

    @Test
    @DisplayName("Should create new ticker with 201 Created and return RFC 7807 409 Conflict on duplicate")
    void shouldCreateTickerAndHandleDuplicates() throws Exception {
        CreateAdminTickerRequest createRequest = new CreateAdminTickerRequest(
                "SMAL11",
                "iShares Small Cap",
                "ETFs",
                "Índices"
        );

        mockMvc.perform(post(TICKERS_BASE_URL)
                        .header("Authorization", "Bearer admin-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.ticker").value("SMAL11"))
                .andExpect(jsonPath("$.data.assetClass").value("ETFs"));

        // Duplicate attempt
        mockMvc.perform(post(TICKERS_BASE_URL)
                        .header("Authorization", "Bearer admin-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate Resource"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("Should update ticker with 200 OK")
    void shouldUpdateTickerSuccessfully() throws Exception {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        CompanyEntity entity = companyJpaRepository.save(CompanyEntity.builder()
                .id(UUID.randomUUID().toString())
                .ticker("WEGE3")
                .name("WEG")
                .assetClass("Ações")
                .sector("Motores")
                .createdAt(now)
                .updatedAt(now)
                .build());

        UpdateAdminTickerRequest updateRequest = new UpdateAdminTickerRequest(
                "WEG S.A.",
                "Ações",
                "Bens Industriais"
        );

        mockMvc.perform(put(TICKERS_BASE_URL + "/" + entity.getId())
                        .header("Authorization", "Bearer admin-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("WEG S.A."))
                .andExpect(jsonPath("$.data.sector").value("Bens Industriais"));
    }

    @Test
    @DisplayName("Should delete ticker with 204 No Content")
    void shouldDeleteTickerSuccessfully() throws Exception {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        CompanyEntity entity = companyJpaRepository.save(CompanyEntity.builder()
                .id(UUID.randomUUID().toString())
                .ticker("TEMP3")
                .name("Empresa Temp")
                .assetClass("Ações")
                .createdAt(now)
                .updatedAt(now)
                .build());

        mockMvc.perform(delete(TICKERS_BASE_URL + "/" + entity.getId())
                        .header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(TICKERS_BASE_URL)
                        .header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", empty()));
    }

    @Test
    @DisplayName("Should clear market-data cache with 200 OK")
    void shouldClearCacheSuccessfully() throws Exception {
        mockMvc.perform(post(TICKERS_BASE_URL + "/cache/clear")
                        .header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value(containsString("limpo com sucesso")));
    }

    @Test
    @DisplayName("Should evict single ticker from cache with 200 OK")
    void shouldEvictSingleTickerCacheSuccessfully() throws Exception {
        mockMvc.perform(post(TICKERS_BASE_URL + "/PETR4/cache/evict")
                        .header("Authorization", "Bearer admin-jwt"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value(containsString("PETR4")));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when creating ticker with non-alphanumeric chars or invalid class")
    void shouldReturn400WhenPayloadInvalid() throws Exception {
        CreateAdminTickerRequest invalidTicker = new CreateAdminTickerRequest("PETR-4!", "Petrobras", "Ações", null);
        mockMvc.perform(post(TICKERS_BASE_URL)
                        .header("Authorization", "Bearer admin-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTicker)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Error"));

        CreateAdminTickerRequest invalidClass = new CreateAdminTickerRequest("PETR4", "Petrobras", "Cripto", null);
        mockMvc.perform(post(TICKERS_BASE_URL)
                        .header("Authorization", "Bearer admin-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidClass)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }
}
