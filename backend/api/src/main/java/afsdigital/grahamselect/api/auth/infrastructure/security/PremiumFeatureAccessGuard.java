package afsdigital.grahamselect.api.auth.infrastructure.security;

import afsdigital.grahamselect.api.user.service.SubscriptionTierService;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class PremiumFeatureAccessGuard {

    private final UserRepository userRepository;
    private final SubscriptionTierService subscriptionTierService;

    public PremiumFeatureAccessGuard(UserRepository userRepository, SubscriptionTierService subscriptionTierService) {
        this.userRepository = userRepository;
        this.subscriptionTierService = subscriptionTierService;
    }

    public boolean canAccessPremiumFeatures() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            throw new PremiumFeatureAccessDeniedException("This feature requires an authenticated Premium subscription.");
        }

        Long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException exception) {
            throw new PremiumFeatureAccessDeniedException("This feature requires a Premium subscription.");
        }

        var user = userRepository.findById(userId)
                .orElseThrow(() -> new PremiumFeatureAccessDeniedException(
                        "This feature requires a Premium subscription."
                ));

        SubscriptionTier effectiveTier = subscriptionTierService.resolveEffectiveTier(user, true);
        if (effectiveTier == SubscriptionTier.PREMIUM || effectiveTier == SubscriptionTier.TRIAL) {
            return true;
        }

        throw new PremiumFeatureAccessDeniedException(
                "This feature requires a Premium subscription. Upgrade your plan to continue."
        );
    }
}
