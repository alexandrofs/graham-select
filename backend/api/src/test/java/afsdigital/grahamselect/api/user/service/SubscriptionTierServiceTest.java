package afsdigital.grahamselect.api.user.service;

import afsdigital.grahamselect.common.user.domain.entities.CommercialPlan;
import afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SubscriptionTierServiceTest {

    @Mock
    private UserRepository userRepository;

    private SubscriptionTierService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionTierService(userRepository);
    }

    @Test
    void shouldReturnPremiumForFounderLifetimeEvenWithPastTrialDate() {
        User founder = User.builder()
                .commercialPlan(CommercialPlan.FOUNDER_LIFETIME)
                .tier(SubscriptionTier.PREMIUM)
                .trialExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(10))
                .build();

        SubscriptionTier result = service.resolveEffectiveTier(founder, true);

        assertThat(result).isEqualTo(SubscriptionTier.PREMIUM);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldReturnTrialForActiveProTrial() {
        User trialUser = User.builder()
                .commercialPlan(CommercialPlan.PRO_TRIAL)
                .tier(SubscriptionTier.TRIAL)
                .trialExpiresAt(LocalDateTime.now(ZoneOffset.UTC).plusDays(45))
                .build();

        SubscriptionTier result = service.resolveEffectiveTier(trialUser, true);

        assertThat(result).isEqualTo(SubscriptionTier.TRIAL);
        verify(userRepository, never()).save(any());
    }

    @Test
    void shouldLazyDowngradeExpiredProTrialToFreeWhenPersistIsTrue() {
        User expiredUser = User.builder()
                .commercialPlan(CommercialPlan.PRO_TRIAL)
                .tier(SubscriptionTier.TRIAL)
                .trialExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1))
                .build();

        SubscriptionTier result = service.resolveEffectiveTier(expiredUser, true);

        assertThat(result).isEqualTo(SubscriptionTier.FREE);
        assertThat(expiredUser.getCommercialPlan()).isEqualTo(CommercialPlan.FREE);
        assertThat(expiredUser.getTier()).isEqualTo(SubscriptionTier.FREE);
        verify(userRepository).save(expiredUser);
    }

    @Test
    void shouldNotPersistWhenPersistLazyDowngradeIsFalse() {
        User expiredUser = User.builder()
                .commercialPlan(CommercialPlan.PRO_TRIAL)
                .tier(SubscriptionTier.TRIAL)
                .trialExpiresAt(LocalDateTime.now(ZoneOffset.UTC).minusDays(1))
                .build();

        SubscriptionTier result = service.resolveEffectiveTier(expiredUser, false);

        assertThat(result).isEqualTo(SubscriptionTier.FREE);
        verify(userRepository, never()).save(any());
    }
}
