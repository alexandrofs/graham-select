package afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.repository;

import afsdigital.grahamselect.valuation.infrastructure.persistence.jpa.entities.IntrinsicValueEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface IntrinsicValueJpaRepository extends JpaRepository<IntrinsicValueEntity, String> {
}
