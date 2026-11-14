package afsdigital.grahamselect.api.family.web;

import afsdigital.grahamselect.api.family.infrastructure.persistence.jpa.repositories.JpaFamilyProfileRepository;
import afsdigital.grahamselect.api.upload.infrastructure.persistence.jpa.repository.B3ImportStatusJpaRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@EmbeddedKafka(topics = TopicConstants.B3_UPLOAD_TOPIC, partitions = 1)
class FamilyProfileControllerIT extends BaseRepositoryIT {

    private static final String ENDPOINT = "/api/v1/family-profiles";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JpaFamilyProfileRepository familyProfileRepository;

    @Autowired
    private B3ImportStatusJpaRepository b3ImportStatusJpaRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setUp() {
        b3ImportStatusJpaRepository.deleteAll();
        familyProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldReturn401WhenUnauthenticated() throws Exception {
        mockMvc.perform(get(ENDPOINT))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403ForFreeUserOnFamilyProfiles() throws Exception {
        User freeUser = saveUser("free-sub", SubscriptionTier.FREE, null);
        mockJwtDecoder("free-token", freeUser);

        mockMvc.perform(get(ENDPOINT)
                        .header("Authorization", "Bearer free-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer free-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"52998224725\",\"name\":\"Maria\",\"relationship\":\"Cônjuge\"}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldLinkListAndUnlinkProfileForPremiumUser() throws Exception {
        User premiumUser = saveUser("premium-sub", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("premium-token", premiumUser);

        MvcResult createResult = mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer premium-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"529.982.247-25\",\"name\":\"Maria Silva\",\"relationship\":\"Cônjuge\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").isNotEmpty())
                .andExpect(jsonPath("$.data.name").value("Maria Silva"))
                .andExpect(jsonPath("$.data.cpfMasked").value("***.982.247-**"))
                .andExpect(jsonPath("$.data.relationship").value("Cônjuge"))
                .andExpect(jsonPath("$.data.active").value(true))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String profileId = createdJson.get("data").get("id").asText();

        mockMvc.perform(get(ENDPOINT)
                        .header("Authorization", "Bearer premium-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(profileId))
                .andExpect(jsonPath("$.meta.total").value(1));

        mockMvc.perform(delete(ENDPOINT + "/" + profileId)
                        .header("Authorization", "Bearer premium-token"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(ENDPOINT)
                        .header("Authorization", "Bearer premium-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(0)))
                .andExpect(jsonPath("$.meta.total").value(0));
    }

    @Test
    void shouldReturn400OnInvalidCpfAnd409OnDuplicate() throws Exception {
        User trialUser = saveUser("trial-sub", SubscriptionTier.TRIAL, LocalDateTime.now(ZoneOffset.UTC).plusDays(7));
        mockJwtDecoder("trial-token", trialUser);

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer trial-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"11111111111\",\"name\":\"João\",\"relationship\":\"Filho\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer trial-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"52998224725\",\"name\":\"Maria\",\"relationship\":\"Cônjuge\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer trial-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"52998224725\",\"name\":\"Maria Outra\",\"relationship\":\"Cônjuge\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldEnforceMasterUserIsolationOnDelete() throws Exception {
        User owner = saveUser("owner-sub", SubscriptionTier.PREMIUM, null);
        User other = saveUser("other-sub", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("owner-token", owner);
        mockJwtDecoder("other-token", other);

        MvcResult createResult = mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer owner-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"52998224725\",\"name\":\"Maria\",\"relationship\":\"Cônjuge\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String profileId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        mockMvc.perform(delete(ENDPOINT + "/" + profileId)
                        .header("Authorization", "Bearer other-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void shouldValidateProfileIdOnB3UploadAndPersistInStatus() throws Exception {
        User freeUser = saveUser("free-upload-sub", SubscriptionTier.FREE, null);
        User premiumUser = saveUser("premium-upload-sub", SubscriptionTier.PREMIUM, null);
        mockJwtDecoder("free-upload-token", freeUser);
        mockJwtDecoder("premium-upload-token", premiumUser);

        byte[] excelBytes = createValidB3Excel();
        MockMultipartFile file = new MockMultipartFile(
                "file", "b3.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );

        // Free user trying to upload with profileId -> 403
        mockMvc.perform(multipart("/api/v1/upload/b3")
                        .file(file)
                        .param("profileId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer free-upload-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        // Premium user with malformed UUID profileId -> 404
        mockMvc.perform(multipart("/api/v1/upload/b3")
                        .file(file)
                        .param("profileId", "not-a-uuid")
                        .header("Authorization", "Bearer premium-upload-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        // Free user trying to delete profile -> 403
        mockMvc.perform(delete(ENDPOINT + "/" + UUID.randomUUID())
                        .header("Authorization", "Bearer free-upload-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        // Premium user with unknown profileId -> 404
        mockMvc.perform(multipart("/api/v1/upload/b3")
                        .file(file)
                        .param("profileId", UUID.randomUUID().toString())
                        .header("Authorization", "Bearer premium-upload-token"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));

        // Create profile for premium user and upload with it -> 202
        MvcResult createResult = mockMvc.perform(post(ENDPOINT)
                        .header("Authorization", "Bearer premium-upload-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cpf\":\"52998224725\",\"name\":\"Maria\",\"relationship\":\"Cônjuge\"}"))
                .andExpect(status().isCreated())
                .andReturn();

        String profileId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("data").get("id").asText();

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/upload/b3")
                        .file(file)
                        .param("profileId", profileId)
                        .header("Authorization", "Bearer premium-upload-token"))
                .andExpect(status().isAccepted())
                .andReturn();

        String correlationId = objectMapper.readTree(uploadResult.getResponse().getContentAsString())
                .get("correlationId").asText();

        mockMvc.perform(get("/api/v1/upload/b3/status/" + correlationId)
                        .header("Authorization", "Bearer premium-upload-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.profileId").value(profileId));
    }

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

    private byte[] createValidB3Excel() throws IOException {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream();
             Workbook wb = new Workbook(os, "Test", "1.0")) {
            Worksheet ws = wb.newWorksheet("Negociação");
            ws.value(0, 0, "Ticker");
            ws.value(0, 1, "Data");
            ws.value(0, 2, "Quantidade");
            ws.value(0, 3, "Preço");
            wb.finish();
            return os.toByteArray();
        }
    }
}

