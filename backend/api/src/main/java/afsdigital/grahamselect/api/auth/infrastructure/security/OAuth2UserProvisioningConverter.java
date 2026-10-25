package afsdigital.grahamselect.api.auth.infrastructure.security;

import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.domain.entities.UserRole;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2UserProvisioningConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        boolean jwtHasAdmin = hasAdminClaim(jwt);

        try {
            User user = userRepository.findByGoogleSub(sub)
                    .map(existingUser -> {
                        boolean changed = false;
                        if (name != null && !name.equals(existingUser.getFullName())) {
                            existingUser.setFullName(name);
                            changed = true;
                        }
                        if (jwtHasAdmin && existingUser.getRole() != UserRole.ADMIN) {
                            existingUser.setRole(UserRole.ADMIN);
                            changed = true;
                        }
                        return changed ? userRepository.save(existingUser) : existingUser;
                    })
                    .orElseGet(() -> {
                        if (email == null) {
                            log.warn("OAuth2 login without email claim for sub: {}", sub);
                        }
                        User newUser = User.builder()
                                .googleSub(sub)
                                .email(email)
                                .fullName(name)
                                .tier(SubscriptionTier.TRIAL)
                                .role(jwtHasAdmin ? UserRole.ADMIN : UserRole.USER)
                                .trialEndsAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(30))
                                .build();
                        log.info("Provisioning new user from OAuth2: {}", email);
                        return userRepository.save(newUser);
                    });

            boolean isAdmin = jwtHasAdmin || user.getRole() == UserRole.ADMIN;
            Collection<GrantedAuthority> authorities = buildAuthorities(isAdmin);
            return new JwtAuthenticationToken(jwt, authorities, user.getId().toString());
        } catch (Exception e) {
            log.error("Error during user provisioning for sub: {}. Retrying lookup.", sub, e);
            // Fallback: search again in case of race condition (another thread saved it)
            User user = userRepository.findByGoogleSub(sub)
                    .orElseThrow(() -> new RuntimeException("User provisioning failed and record not found", e));
            boolean isAdmin = jwtHasAdmin || user.getRole() == UserRole.ADMIN;
            Collection<GrantedAuthority> authorities = buildAuthorities(isAdmin);
            return new JwtAuthenticationToken(jwt, authorities, user.getId().toString());
        }
    }

    private boolean hasAdminClaim(Jwt jwt) {
        String roleClaim = jwt.getClaimAsString("role");
        if ("ADMIN".equalsIgnoreCase(roleClaim) || "ROLE_ADMIN".equalsIgnoreCase(roleClaim)) {
            return true;
        }
        List<String> rolesClaim = jwt.getClaimAsStringList("roles");
        if (rolesClaim != null) {
            return rolesClaim.stream()
                    .anyMatch(r -> "ADMIN".equalsIgnoreCase(r) || "ROLE_ADMIN".equalsIgnoreCase(r));
        }
        return false;
    }

    private Collection<GrantedAuthority> buildAuthorities(boolean isAdmin) {
        return isAdmin
                ? Set.of(new SimpleGrantedAuthority("ROLE_USER"), new SimpleGrantedAuthority("ROLE_ADMIN"))
                : Set.of(new SimpleGrantedAuthority("ROLE_USER"));
    }
}
