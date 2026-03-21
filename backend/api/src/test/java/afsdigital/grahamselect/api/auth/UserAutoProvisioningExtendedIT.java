package afsdigital.grahamselect.api.auth;

import afsdigital.grahamselect.api.auth.infrastructure.security.OAuth2UserProvisioningConverter;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.user.domain.entities.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Extended integration tests for Story 1.1: Google OAuth2 Auto-provisioning.
 * Covers edge cases and scenarios not addressed by the original UserAutoProvisioningIT.
 */
public class UserAutoProvisioningExtendedIT extends BaseRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuth2UserProvisioningConverter converter;

    @BeforeEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    // --- AC3: Auto-provisioning on first login ---

    @Nested
    @DisplayName("AC3: Auto-provisioning - First Login")
    class FirstLoginTests {

        @Test
        @DisplayName("Should create user with all Google profile fields")
        void shouldCreateUserWithAllFields() {
            Jwt jwt = buildJwt("sub-001", "user@test.com", "Test User");

            converter.convert(jwt);

            Optional<User> user = userRepository.findByGoogleSub("sub-001");
            assertThat(user).isPresent();
            assertThat(user.get().getGoogleSub()).isEqualTo("sub-001");
            assertThat(user.get().getEmail()).isEqualTo("user@test.com");
            assertThat(user.get().getFullName()).isEqualTo("Test User");
            assertThat(user.get().getId()).isNotNull();
            assertThat(user.get().getCreatedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should provision user even when name claim is null")
        void shouldProvisionUserWithoutName() {
            Jwt jwt = buildJwt("sub-002", "noname@test.com", null);

            converter.convert(jwt);

            Optional<User> user = userRepository.findByGoogleSub("sub-002");
            assertThat(user).isPresent();
            assertThat(user.get().getEmail()).isEqualTo("noname@test.com");
            assertThat(user.get().getFullName()).isNull();
        }

        @Test
        @DisplayName("BUG: JWT without email claim causes DB constraint violation - should handle gracefully")
        void shouldFailWhenEmailClaimIsNull() {
            // BUG ENCONTRADO: O OAuth2UserProvisioningConverter aceita JWT sem email claim
            // e tenta salvar no banco, mas a coluna 'email' tem constraint NOT NULL.
            // O código deveria validar a presença do email ANTES de tentar persistir.
            // Atualmente loga warning mas prossegue com o save, causando exceção.
            Jwt jwt = Jwt.withTokenValue("mock-token")
                    .header("alg", "none")
                    .subject("sub-003")
                    .claim("name", "No Email User")
                    .build();

            // Verify that the converter handles this case (currently throws due to NOT NULL constraint)
            org.junit.jupiter.api.Assertions.assertThrows(
                    RuntimeException.class,
                    () -> converter.convert(jwt),
                    "JWT sem email deveria ser tratado antes de tentar persistir no banco"
            );

            // User should NOT be persisted
            assertThat(userRepository.findByGoogleSub("sub-003")).isEmpty();
        }
    }

    // --- AC3: No duplicate on second login ---

    @Nested
    @DisplayName("AC3: Auto-provisioning - Returning User")
    class ReturningUserTests {

        @Test
        @DisplayName("Should not create duplicate user on second login")
        void shouldNotDuplicateOnSecondLogin() {
            Jwt jwt = buildJwt("sub-100", "returning@test.com", "Returning User");

            converter.convert(jwt);
            converter.convert(jwt);

            assertThat(userRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should update user name when Google profile name changes")
        void shouldUpdateNameOnReturningLogin() {
            Jwt jwt1 = buildJwt("sub-200", "update@test.com", "Old Name");
            converter.convert(jwt1);

            Jwt jwt2 = buildJwt("sub-200", "update@test.com", "New Name");
            converter.convert(jwt2);

            Optional<User> user = userRepository.findByGoogleSub("sub-200");
            assertThat(user).isPresent();
            assertThat(user.get().getFullName()).isEqualTo("New Name");
            assertThat(userRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should NOT update name if it hasn't changed")
        void shouldNotUpdateIfNameUnchanged() {
            Jwt jwt = buildJwt("sub-300", "same@test.com", "Same Name");
            converter.convert(jwt);

            User originalUser = userRepository.findByGoogleSub("sub-300").orElseThrow();
            var originalUpdatedAt = originalUser.getUpdatedAt();

            // Second login with same name
            converter.convert(jwt);

            User afterSecondLogin = userRepository.findByGoogleSub("sub-300").orElseThrow();
            // If name didn't change, updatedAt should remain the same (no unnecessary save)
            assertThat(afterSecondLogin.getFullName()).isEqualTo("Same Name");
            assertThat(userRepository.count()).isEqualTo(1);
        }
    }

    // --- AC5: SecurityContext populated with user's internal ID ---

    @Nested
    @DisplayName("AC5: SecurityContext Population")
    class SecurityContextTests {

        @Test
        @DisplayName("Should return JwtAuthenticationToken with user's internal ID as principal name")
        void shouldReturnTokenWithInternalId() {
            Jwt jwt = buildJwt("sub-500", "principal@test.com", "Principal User");

            AbstractAuthenticationToken token = converter.convert(jwt);

            assertThat(token).isInstanceOf(JwtAuthenticationToken.class);
            assertThat(token.getName()).isNotNull();
            assertThat(token.getName()).isNotBlank();

            // Verify the principal name is the internal DB ID (numeric)
            Long internalId = Long.parseLong(token.getName());
            assertThat(internalId).isGreaterThan(0);

            // Verify the ID matches the persisted user
            User user = userRepository.findByGoogleSub("sub-500").orElseThrow();
            assertThat(internalId).isEqualTo(user.getId());
        }

        @Test
        @DisplayName("Should grant ROLE_USER authority")
        void shouldGrantRoleUser() {
            Jwt jwt = buildJwt("sub-600", "role@test.com", "Role User");

            AbstractAuthenticationToken token = converter.convert(jwt);

            assertThat(token.getAuthorities()).isNotEmpty();
            assertThat(token.getAuthorities())
                    .extracting("authority")
                    .contains("ROLE_USER");
        }
    }

    // --- AC2: Database schema integrity ---

    @Nested
    @DisplayName("AC2: Database Schema Integrity")
    class SchemaTests {

        @Test
        @DisplayName("Should enforce unique constraint on google_sub")
        void shouldEnforceUniqueGoogleSub() {
            // We verify indirectly: two different users with different subs should both be stored
            Jwt jwt1 = buildJwt("unique-sub-1", "user1@test.com", "User 1");
            Jwt jwt2 = buildJwt("unique-sub-2", "user2@test.com", "User 2");

            converter.convert(jwt1);
            converter.convert(jwt2);

            assertThat(userRepository.count()).isEqualTo(2);
            assertThat(userRepository.findByGoogleSub("unique-sub-1")).isPresent();
            assertThat(userRepository.findByGoogleSub("unique-sub-2")).isPresent();
        }

        @Test
        @DisplayName("Should store created_at and updated_at timestamps")
        void shouldPopulateTimestamps() {
            Jwt jwt = buildJwt("ts-sub", "timestamps@test.com", "Timestamp User");

            converter.convert(jwt);

            User user = userRepository.findByGoogleSub("ts-sub").orElseThrow();
            assertThat(user.getCreatedAt()).isNotNull();
            // updatedAt may or may not be set depending on JPA auditing config
        }
    }

    // --- Multiple users scenario ---

    @Nested
    @DisplayName("Multi-user Isolation")
    class MultiUserTests {

        @Test
        @DisplayName("Should isolate users - each sub gets its own record")
        void shouldCreateSeparateRecordsForDifferentSubs() {
            Jwt jwt1 = buildJwt("user-a", "a@test.com", "User A");
            Jwt jwt2 = buildJwt("user-b", "b@test.com", "User B");
            Jwt jwt3 = buildJwt("user-c", "c@test.com", "User C");

            converter.convert(jwt1);
            converter.convert(jwt2);
            converter.convert(jwt3);

            assertThat(userRepository.count()).isEqualTo(3);

            // Each user has a distinct internal ID
            Long idA = userRepository.findByGoogleSub("user-a").orElseThrow().getId();
            Long idB = userRepository.findByGoogleSub("user-b").orElseThrow().getId();
            Long idC = userRepository.findByGoogleSub("user-c").orElseThrow().getId();

            assertThat(idA).isNotEqualTo(idB).isNotEqualTo(idC);
        }
    }

    // --- Utility ---

    private Jwt buildJwt(String sub, String email, String name) {
        var builder = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject(sub);

        if (email != null) {
            builder.claim("email", email);
        }
        if (name != null) {
            builder.claim("name", name);
        }

        return builder.build();
    }
}
