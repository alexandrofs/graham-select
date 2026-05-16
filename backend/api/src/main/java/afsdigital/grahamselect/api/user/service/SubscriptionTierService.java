package afsdigital.grahamselect.api.user.service;

import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class SubscriptionTierService {

    private final UserRepository userRepository;

    @Transactional
    public SubscriptionTier resolveEffectiveTier(User user, boolean persistLazyDowngrade) {
        if (user.getTier() != SubscriptionTier.TRIAL) {
            return user.getTier();
        }

        if (user.getTrialEndsAt() != null && user.getTrialEndsAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            if (persistLazyDowngrade) {
                user.setTier(SubscriptionTier.FREE);
                userRepository.save(user);
            }
            return SubscriptionTier.FREE;
        }

        return SubscriptionTier.TRIAL;
    }
}
