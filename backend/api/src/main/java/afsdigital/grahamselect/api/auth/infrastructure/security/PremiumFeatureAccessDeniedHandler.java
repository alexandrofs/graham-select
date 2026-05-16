package afsdigital.grahamselect.api.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;

@Component
@RequiredArgsConstructor
public class PremiumFeatureAccessDeniedHandler implements AccessDeniedHandler {

    private static final URI UPGRADE_REQUIRED_TYPE = URI.create("urn:problem-type:upgrade-required");
    private static final URI ACCESS_DENIED_TYPE = URI.create("urn:problem-type:access-denied");

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
        problemDetail.setStatus(HttpStatus.FORBIDDEN.value());
        problemDetail.setInstance(URI.create(request.getRequestURI()));

        if (accessDeniedException instanceof PremiumFeatureAccessDeniedException) {
            problemDetail.setType(UPGRADE_REQUIRED_TYPE);
            problemDetail.setTitle("Upgrade Required");
            problemDetail.setDetail(accessDeniedException.getMessage());
            problemDetail.setProperty("requiredTier", "PREMIUM");
        } else {
            problemDetail.setType(ACCESS_DENIED_TYPE);
            problemDetail.setTitle("Access Denied");
            problemDetail.setDetail("You do not have permission to access this resource.");
        }

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }
}
