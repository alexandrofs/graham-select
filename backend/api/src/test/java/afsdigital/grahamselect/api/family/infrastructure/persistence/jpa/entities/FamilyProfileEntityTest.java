package afsdigital.grahamselect.api.family.infrastructure.persistence.jpa.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FamilyProfileEntityTest {

    @Test
    @DisplayName("Nova entidade construída via builder deve indicar isNew=true por padrão")
    void shouldBeNewByDefault() {
        UUID id = UUID.randomUUID();
        FamilyProfileEntity entity = FamilyProfileEntity.builder()
                .id(id)
                .masterUserId("user-123")
                .cpfDigits("12345678901")
                .name("Nome Teste")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.isNew()).isTrue();
    }

    @Test
    @DisplayName("Entidade construída com updatedAt existente (update) deve refletir isNew=false")
    void shouldReflectIsNewFalseWhenConfiguredForUpdate() {
        FamilyProfileEntity entity = FamilyProfileEntity.builder()
                .id(UUID.randomUUID())
                .masterUserId("user-123")
                .cpfDigits("12345678901")
                .name("Nome Teste")
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isNew(false)
                .build();

        assertThat(entity.isNew()).isFalse();
    }

    @Test
    @DisplayName("Após callbacks @PostLoad/@PostPersist, isNew deve ser alterado para false")
    void shouldMarkNotNewAfterPostLoadOrPostPersist() {
        FamilyProfileEntity entity = FamilyProfileEntity.builder()
                .id(UUID.randomUUID())
                .masterUserId("user-123")
                .cpfDigits("12345678901")
                .name("Nome Teste")
                .active(true)
                .createdAt(LocalDateTime.now())
                .build();

        assertThat(entity.isNew()).isTrue();

        entity.markNotNew();

        assertThat(entity.isNew()).isFalse();
    }
}
