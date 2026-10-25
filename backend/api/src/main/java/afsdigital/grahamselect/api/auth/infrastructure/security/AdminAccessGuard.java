package afsdigital.grahamselect.api.auth.infrastructure.security;

import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.domain.entities.UserRole;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAccessGuard {

    private final UserRepository userRepository;

    public boolean canAccessAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getName() == null) {
            AccessDeniedException ex = new AccessDeniedException("Admin privileges required.");
            log.error("Access denied to admin resource: unauthenticated principal", ex);
            throw ex;
        }

        if (authentication.getAuthorities() != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()) || "ADMIN".equalsIgnoreCase(a.getAuthority()))) {
            return true;
        }

        if (authentication.getPrincipal() instanceof Jwt jwt && hasAdminClaim(jwt)) {
            return true;
        }

        Optional<User> userOpt = findUserByPrincipal(authentication.getName());
        if (userOpt.isPresent() && userOpt.get().getRole() == UserRole.ADMIN) {
            return true;
        }

        log.warn("Access denied to admin resource for principal: {}", authentication.getName());
        AccessDeniedException ex = new AccessDeniedException("Access restricted to system administrators.");
        log.error("Forbidden admin access attempt by principal: {}", authentication.getName(), ex);
        throw ex;
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

    private Optional<User> findUserByPrincipal(String principalName) {
        try {
            Long userId = Long.parseLong(principalName);
            return userRepository.findById(userId);
        } catch (NumberFormatException e) {
            return userRepository.findByGoogleSub(principalName);
        }
    }
}
