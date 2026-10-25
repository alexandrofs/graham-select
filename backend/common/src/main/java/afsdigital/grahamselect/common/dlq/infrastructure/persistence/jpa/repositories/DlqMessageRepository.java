package afsdigital.grahamselect.common.dlq.infrastructure.persistence.jpa.repositories;

import afsdigital.grahamselect.common.dlq.domain.entities.DlqMessageStatus;
import afsdigital.grahamselect.common.dlq.infrastructure.persistence.jpa.entities.DlqMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DlqMessageRepository extends JpaRepository<DlqMessageEntity, Long> {

    List<DlqMessageEntity> findByStatusOrderByCreatedAtDesc(DlqMessageStatus status);

    List<DlqMessageEntity> findAllByOrderByCreatedAtDesc();

    Optional<DlqMessageEntity> findByEventId(String eventId);

    long countByStatus(DlqMessageStatus status);

    List<DlqMessageEntity> findByIdIn(List<Long> ids);
}
