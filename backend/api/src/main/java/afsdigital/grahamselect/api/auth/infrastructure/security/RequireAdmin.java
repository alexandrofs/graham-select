package afsdigital.grahamselect.api.auth.infrastructure.security;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to restrict access to administrative endpoints.
 * Access is granted if the user has ROLE_ADMIN in JWT claims or in the database.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("@adminAccessGuard.canAccessAdmin()")
public @interface RequireAdmin {
}
