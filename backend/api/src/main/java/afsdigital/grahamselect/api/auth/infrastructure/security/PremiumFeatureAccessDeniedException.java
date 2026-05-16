package afsdigital.grahamselect.api.auth.infrastructure.security;

import org.springframework.security.access.AccessDeniedException;

public class PremiumFeatureAccessDeniedException extends AccessDeniedException {

    public PremiumFeatureAccessDeniedException(String message) {
        super(message);
    }
}
