package afsdigital.grahamselect.api.portfolio.web;

import afsdigital.grahamselect.api.portfolio.infrastructure.sse.SseNotificationAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final SseNotificationAdapter sseNotificationAdapter;

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamNotifications(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        log.info("Creating SSE emitter for user: {}", userId);
        return sseNotificationAdapter.createEmitter(userId);
    }
}
