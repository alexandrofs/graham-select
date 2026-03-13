package afsdigital.grahamselect.api.auth;

import afsdigital.grahamselect.api.auth.infrastructure.security.OAuth2UserProvisioningConverter;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import afsdigital.grahamselect.api.valuation.infrastructure.persistence.BaseRepositoryIT;
import afsdigital.grahamselect.common.user.domain.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class UserAutoProvisioningIT extends BaseRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OAuth2UserProvisioningConverter converter;

    @Test
    @DisplayName("Should create user on first login with Google OAuth2 JWT")
    void shouldCreateUserOnFirstLogin() {
        String googleId = "google-user-123";
        String email = "alex@example.com";
        String name = "Alex Silva";

        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .subject(googleId)
                .claim("email", email)
                .claim("name", name)
                .build();

        // Act - Call the converter directly
        converter.convert(jwt);

        // Assert - Verify provisioning
        Optional<User> user = userRepository.findByGoogleSub(googleId);
        assertThat(user).isPresent();
        assertThat(user.get().getEmail()).isEqualTo(email);
        assertThat(user.get().getFullName()).isEqualTo(name);
        assertThat(user.get().getGoogleSub()).isEqualTo(googleId);

        // Second call should not duplicate
        converter.convert(jwt);
        assertThat(userRepository.count()).isEqualTo(1);
    }
}
