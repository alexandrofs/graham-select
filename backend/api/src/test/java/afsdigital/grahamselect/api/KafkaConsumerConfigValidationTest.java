package afsdigital.grahamselect.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

class KafkaConsumerConfigValidationTest {

    @Test
    void shouldResolveKafkaConsumerGroupIdFromApplicationYaml() throws Exception {
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        List<PropertySource<?>> sources = loader.load("application.yml", new ClassPathResource("application.yml"));

        StandardEnvironment env = new StandardEnvironment();
        sources.forEach(env.getPropertySources()::addLast);

        String groupId = assertDoesNotThrow(
                () -> env.resolveRequiredPlaceholders("${spring.kafka.consumer.group-id}"),
                "spring.kafka.consumer.group-id deve estar configurado em application.yml"
        );
        assertFalse(groupId.isBlank(), "spring.kafka.consumer.group-id não pode ser vazio");
    }
}
