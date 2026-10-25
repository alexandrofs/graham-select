package afsdigital.grahamselect.common.user.infrastructure.persistence;

import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByGoogleSub(String googleSub);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.googleSub = :googleSub")
    Optional<User> findByGoogleSubForWrite(@Param("googleSub") String googleSub);

    long countByTier(SubscriptionTier tier);

    @Query("SELECT COUNT(u) FROM User u WHERE u.createdAt >= :since OR u.updatedAt >= :since")
    long countActiveUsersSince(@Param("since") LocalDateTime since);

    @Query("SELECT u.id FROM User u WHERE u.createdAt >= :since OR u.updatedAt >= :since")
    List<Long> findActiveUserIdsSince(@Param("since") LocalDateTime since);
}
