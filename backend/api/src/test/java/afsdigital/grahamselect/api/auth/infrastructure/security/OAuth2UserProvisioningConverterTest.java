package afsdigital.grahamselect.api.auth.infrastructure.security;

import afsdigital.grahamselect.common.founders.application.service.FounderQuotaService;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OAuth2UserProvisioningConverterTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private FounderQuotaService founderQuotaService;
    @Mock
    private PlatformTransactionManager transactionManager;

    @Test
    void shouldRejectJwtWithMissingOrBlankSubClaim() {
        OAuth2UserProvisioningConverter converter = new OAuth2UserProvisioningConverter(
                userRepository, founderQuotaService, transactionManager
        );

        Jwt jwtMissingSub = new Jwt(
                "token-value",
                Instant.now(),
                Instant.now().plusSeconds(300),
                Map.of("alg", "RS256"),
                Map.of("email", "user@example.com")
        );

        assertThatThrownBy(() -> converter.convert(jwtMissingSub))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("sub");
        verifyNoInteractions(userRepository);
    }
}
