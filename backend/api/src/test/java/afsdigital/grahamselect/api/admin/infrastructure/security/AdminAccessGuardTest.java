package afsdigital.grahamselect.api.admin.infrastructure.security;

import afsdigital.grahamselect.api.auth.infrastructure.security.AdminAccessGuard;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.domain.entities.UserRole;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccessGuardTest {

    @Mock
    private UserRepository userRepository;

    private AdminAccessGuard adminAccessGuard;

    @BeforeEach
    void setUp() {
        adminAccessGuard = new AdminAccessGuard(userRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should grant access when principal has ROLE_ADMIN authority")
    void shouldGrantAccessWhenPrincipalHasRoleAdminAuthority() {
        var auth = new UsernamePasswordAuthenticationToken(
                "1",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(adminAccessGuard.canAccessAdmin()).isTrue();
    }

    @Test
    @DisplayName("Should grant access when JWT has role=ADMIN claim")
    void shouldGrantAccessWhenJwtHasAdminClaim() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("sub-admin")
                .claim("role", "ADMIN")
                .build();
        var auth = new JwtAuthenticationToken(jwt, Set.of(new SimpleGrantedAuthority("ROLE_USER")), "10");
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(adminAccessGuard.canAccessAdmin()).isTrue();
    }

    @Test
    @DisplayName("Should grant access when database user has UserRole.ADMIN")
    void shouldGrantAccessWhenUserHasAdminRoleInDatabase() {
        var auth = new UsernamePasswordAuthenticationToken(
                "42",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        User adminUser = User.builder()
                .id(42L)
                .googleSub("sub-42")
                .email("admin@graham.com")
                .tier(SubscriptionTier.PREMIUM)
                .role(UserRole.ADMIN)
                .build();
        when(userRepository.findById(42L)).thenReturn(Optional.of(adminUser));

        assertThat(adminAccessGuard.canAccessAdmin()).isTrue();
    }

    @Test
    @DisplayName("Should deny access with AccessDeniedException when user is regular USER")
    void shouldDenyAccessWhenUserIsRegularUser() {
        var auth = new UsernamePasswordAuthenticationToken(
                "99",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        User regularUser = User.builder()
                .id(99L)
                .googleSub("sub-99")
                .email("user@graham.com")
                .tier(SubscriptionTier.PREMIUM)
                .role(UserRole.USER)
                .build();
        when(userRepository.findById(99L)).thenReturn(Optional.of(regularUser));

        assertThatThrownBy(() -> adminAccessGuard.canAccessAdmin())
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access restricted to system administrators");
    }
}
