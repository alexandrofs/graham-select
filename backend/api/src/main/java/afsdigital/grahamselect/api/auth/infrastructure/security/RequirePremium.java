package afsdigital.grahamselect.api.auth.infrastructure.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to restrict access to Premium features.
 * Access is granted if the user has a PREMIUM tier or an active TRIAL tier.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@premiumFeatureAccessGuard.canAccessPremiumFeatures()")
public @interface RequirePremium {
}
