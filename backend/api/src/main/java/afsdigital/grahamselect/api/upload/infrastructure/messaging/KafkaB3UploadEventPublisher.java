package afsdigital.grahamselect.api.upload.infrastructure.messaging;

import afsdigital.grahamselect.common.domain.entities.TopicConstants;
import afsdigital.grahamselect.common.upload.application.repository.B3UploadEventPort;
import afsdigital.grahamselect.common.upload.domain.events.FileUploadedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaB3UploadEventPublisher implements B3UploadEventPort {

    private final KafkaTemplate<String, FileUploadedEvent> kafkaTemplate;

    @Override
    public void publish(FileUploadedEvent event) {
        kafkaTemplate.send(TopicConstants.B3_UPLOAD_TOPIC, event.userId(), event);
    }
}
