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
        if (userId == null) return new SseEmitter();
        String key = userId.trim().toLowerCase();
        SseEmitter emitter = new SseEmitter(0L); // sem timeout
        userEmitters.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(emitter);
        emitter.onCompletion(() -> removeEmitter(key, emitter));
        emitter.onTimeout(() -> removeEmitter(key, emitter));
        emitter.onError(e -> removeEmitter(key, emitter));
        log.info("Created SSE emitter for user: {}. Total for user: {}", userId, userEmitters.get(key).size());
        return emitter;
    }
    
    @Override
    public void sendPortfolioUpdate(String userId) {
        sendNotification(userId, "PORTFOLIO_UPDATED", Map.of("userId", userId != null ? userId : "unknown", "timestamp", OffsetDateTime.now().toString()));
    }

    @Override
    public void sendNotification(String userId, String eventName, Object payload) {
        if (userId == null) return;
        String key = userId.trim().toLowerCase();
        List<SseEmitter> emitters = userEmitters.getOrDefault(key, List.of());
        log.info("Sending notification '{}' to {} emitters for user: {}", eventName, emitters.size(), userId);
        
        // CopyOnWriteArrayList is thread-safe for iteration
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .name(eventName)
                        .data(payload)
                );
            } catch (IOException e) {
                log.warn("Failed to send SSE event '{}' to user {}: {}", eventName, userId, e.getMessage());
                removeEmitter(key, emitter);
            }
        });
    }
    
    private void removeEmitter(String key, SseEmitter emitter) {
        if (key == null) return;
        List<SseEmitter> emitters = userEmitters.get(key);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(key);
            }
            log.info("Removed SSE emitter for key: {}. Remaining for user: {}", key, emitters.size());
        }
    }
}
