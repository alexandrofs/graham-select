package afsdigital.grahamselect.api.user.web;

import afsdigital.grahamselect.api.UsersApiDelegate;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import afsdigital.grahamselect.model.SubscriptionTier;
import afsdigital.grahamselect.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class UserController implements UsersApiDelegate {

    private final UserRepository userRepository;

    @Override
    public ResponseEntity<UserProfile> usersMeGet() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            Long userId = Long.parseLong(authentication.getName());
            return userRepository.findById(userId)
                    .map(this::mapToUserProfile)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (NumberFormatException e) {
            return ResponseEntity.status(401).build();
        }
    }

    private UserProfile mapToUserProfile(User user) {
        UserProfile profile = new UserProfile();
        profile.setId(user.getId());
        profile.setEmail(user.getEmail());
        profile.setFullName(user.getFullName());

        var domainTier = user.getTier();
        if (domainTier == afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier.TRIAL
                && user.getTrialEndsAt() != null
                && user.getTrialEndsAt().isBefore(LocalDateTime.now())) {
            domainTier = afsdigital.grahamselect.common.user.domain.entities.SubscriptionTier.FREE;
        }

        profile.setTier(SubscriptionTier.fromValue(domainTier.name()));

        if (user.getTrialEndsAt() != null) {
            profile.setTrialEndsAt(user.getTrialEndsAt().atOffset(ZoneOffset.UTC));
            long daysRemaining = Duration.between(LocalDateTime.now(), user.getTrialEndsAt()).toDays();
            profile.setDaysRemaining((int) Math.max(0, daysRemaining));
        }

        return profile;
    }
}
