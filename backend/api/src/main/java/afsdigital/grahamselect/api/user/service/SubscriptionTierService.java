package afsdigital.grahamselect.api.user.service;

import afsdigital.grahamselect.common.user.domain.entities.CommercialPlan;
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
        if (user.getCommercialPlan() == CommercialPlan.FOUNDER_LIFETIME
                || user.getTier() == SubscriptionTier.PREMIUM) {
            if (persistLazyDowngrade && (user.getCommercialPlan() != CommercialPlan.FOUNDER_LIFETIME
                    || user.getTier() != SubscriptionTier.PREMIUM)) {
                user.setCommercialPlan(CommercialPlan.FOUNDER_LIFETIME);
                user.setTier(SubscriptionTier.PREMIUM);
                user.setTrialEndsAt(null);
                user.setTrialExpiresAt(null);
                userRepository.save(user);
            }
            return SubscriptionTier.PREMIUM;
        }

        if (user.getCommercialPlan() == CommercialPlan.FREE
                || user.getTier() == SubscriptionTier.FREE) {
            if (persistLazyDowngrade && (user.getCommercialPlan() != CommercialPlan.FREE
                    || user.getTier() != SubscriptionTier.FREE)) {
                user.setCommercialPlan(CommercialPlan.FREE);
                user.setTier(SubscriptionTier.FREE);
                userRepository.save(user);
            }
            return SubscriptionTier.FREE;
        }

        LocalDateTime expiration = user.getTrialExpiresAt() != null
                ? user.getTrialExpiresAt()
                : user.getTrialEndsAt();

        if (expiration != null && expiration.isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
            if (persistLazyDowngrade) {
                user.setCommercialPlan(CommercialPlan.FREE);
                user.setTier(SubscriptionTier.FREE);
                userRepository.save(user);
            }
            return SubscriptionTier.FREE;
        }

        return SubscriptionTier.TRIAL;
    }
}
