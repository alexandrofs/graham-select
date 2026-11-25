package afsdigital.grahamselect.api.auth.infrastructure.security;

import afsdigital.grahamselect.common.founders.application.service.FounderQuotaService;
import afsdigital.grahamselect.common.user.domain.entities.CommercialPlan;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.domain.entities.UserRole;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

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
    private final FounderQuotaService founderQuotaService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        String sub = jwt.getSubject();
        String email = jwt.getClaimAsString("email");
        String name = jwt.getClaimAsString("name");
        boolean jwtHasAdmin = hasAdminClaim(jwt);

        User existing = userRepository.findByGoogleSub(sub).orElse(null);
        if (existing != null) {
            User updated = updateExistingIfNeeded(existing, name, jwtHasAdmin);
            return buildAuthToken(jwt, updated, jwtHasAdmin);
        }

        if (email == null || email.isBlank()) {
            log.error("OAuth2 login rejected: missing email claim for sub: {}", sub);
            throw new BadCredentialsException("OAuth2 JWT missing required email claim");
        }

        try {
            TransactionTemplate requiresNewTx = new TransactionTemplate(transactionManager);
            requiresNewTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            User created = requiresNewTx.execute(status -> provisionNewUser(sub, email, name, jwtHasAdmin));
            return buildAuthToken(jwt, created, jwtHasAdmin);
        } catch (Exception e) {
            log.error("Error during user provisioning for sub: {}. Retrying lookup.", sub, e);
            // Fallback: search again in case of race condition (another thread saved it)
            User user = userRepository.findByGoogleSub(sub)
                    .orElseThrow(() -> new RuntimeException("User provisioning failed and record not found", e));
            return buildAuthToken(jwt, user, jwtHasAdmin);
        }
    }

    private User updateExistingIfNeeded(User existingUser, String name, boolean jwtHasAdmin) {
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
    }

    private User provisionNewUser(String sub, String email, String name, boolean jwtHasAdmin) {
        return userRepository.findByGoogleSub(sub)
                .map(existingUser -> updateExistingIfNeeded(existingUser, name, jwtHasAdmin))
                .orElseGet(() -> {
                    boolean claimedFounderSlot = founderQuotaService.tryClaimFounderSlot();
                    CommercialPlan commercialPlan;
                    SubscriptionTier tier;
                    LocalDateTime trialExpiresAt = null;

                    if (claimedFounderSlot) {
                        commercialPlan = CommercialPlan.FOUNDER_LIFETIME;
                        tier = SubscriptionTier.PREMIUM;
                    } else {
                        commercialPlan = CommercialPlan.PRO_TRIAL;
                        tier = SubscriptionTier.TRIAL;
                        trialExpiresAt = founderQuotaService.calculateProTrialExpiration(LocalDateTime.now(ZoneOffset.UTC));
                    }

                    User newUser = User.builder()
                            .googleSub(sub)
                            .email(email)
                            .fullName(name)
                            .tier(tier)
                            .commercialPlan(commercialPlan)
                            .role(jwtHasAdmin ? UserRole.ADMIN : UserRole.USER)
                            .trialEndsAt(trialExpiresAt)
                            .trialExpiresAt(trialExpiresAt)
                            .build();
                    log.info("Provisioning new user from OAuth2 with commercialPlan={}", commercialPlan);
                    return userRepository.saveAndFlush(newUser);
                });
    }

    private AbstractAuthenticationToken buildAuthToken(Jwt jwt, User user, boolean jwtHasAdmin) {
        boolean isAdmin = jwtHasAdmin || user.getRole() == UserRole.ADMIN;
        Collection<GrantedAuthority> authorities = buildAuthorities(isAdmin);
        return new JwtAuthenticationToken(jwt, authorities, user.getId().toString());
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
