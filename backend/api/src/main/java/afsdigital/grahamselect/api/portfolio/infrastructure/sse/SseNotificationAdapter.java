package afsdigital.grahamselect.api.portfolio.infrastructure.sse;

import afsdigital.grahamselect.common.portfolio.application.repository.NotificationPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class SseNotificationAdapter implements NotificationPort {
    private final Map<String, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();
    
    public SseEmitter createEmitter(String userId) {
        SseEmitter emitter = new SseEmitter(0L); // sem timeout
        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> removeEmitter(userId, emitter));
        emitter.onError(e -> removeEmitter(userId, emitter));
        return emitter;
    }
    
    @Override
    public void sendPortfolioUpdate(String userId) {
        List<SseEmitter> emitters = userEmitters.getOrDefault(userId, List.of());
        log.info("Sending portfolio update to {} emitters for user: {}", emitters.size(), userId);
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("PORTFOLIO_UPDATED")
                    .data(Map.of("userId", userId, "timestamp", OffsetDateTime.now().toString()))
                );
            } catch (IOException e) {
                log.warn("Failed to send SSE event to user {}: {}", userId, e.getMessage());
                removeEmitter(userId, emitter);
            }
        });
    }
    
    private void removeEmitter(String userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }
}
