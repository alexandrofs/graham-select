package afsdigital.grahamselect.api.user.web;

import afsdigital.grahamselect.api.UsersApiDelegate;
import afsdigital.grahamselect.api.user.service.SubscriptionTierService;
import afsdigital.grahamselect.common.user.application.usecase.CancelAccountDeletionUseCase;
import afsdigital.grahamselect.common.user.application.usecase.RequestAccountDeletionUseCase;
import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import afsdigital.grahamselect.model.DeletionRequestResponse;
import afsdigital.grahamselect.model.DeletionRequestResponseData;
import afsdigital.grahamselect.model.SubscriptionTier;
import afsdigital.grahamselect.model.UpdateProfileRequest;
import afsdigital.grahamselect.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserController implements UsersApiDelegate {

    private final UserRepository userRepository;
    private final SubscriptionTierService subscriptionTierService;
    private final RequestAccountDeletionUseCase requestAccountDeletionUseCase;
    private final CancelAccountDeletionUseCase cancelAccountDeletionUseCase;

    @Override
    public ResponseEntity<UserProfile> usersMeGet() {
        return getAuthenticatedUserId()
                .flatMap(userRepository::findById)
                .map(this::mapToUserProfile)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).build());
    }

    @Override
    public ResponseEntity<UserProfile> usersProfilePatch(UpdateProfileRequest updateProfileRequest) {
        return getAuthenticatedUserId()
                .flatMap(userRepository::findById)
                .map(user -> {
                    if (updateProfileRequest.getInvestorProfile() != null) {
                        user.setInvestorProfile(afsdigital.grahamselect.common.user.domain.entities.InvestorProfile.valueOf(updateProfileRequest.getInvestorProfile().name()));
                    }
                    return userRepository.save(user);
                })
                .map(this::mapToUserProfile)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.status(401).build());
    }

    @Override
    public ResponseEntity<DeletionRequestResponse> usersMeDelete() {
        Optional<Long> userIdOpt = getAuthenticatedUserId();
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        AccountDeletionRequest request = requestAccountDeletionUseCase.execute(userIdOpt.get());

        DeletionRequestResponse response = new DeletionRequestResponse();
        DeletionRequestResponseData data = new DeletionRequestResponseData();
        data.setRequestId(request.getId());
        data.setStatus(request.getStatus().name());
        data.setEstimatedCompletionWithin("24h");
        response.setData(data);

        return ResponseEntity.accepted().body(response);
    }

    @Override
    public ResponseEntity<Void> usersMeCancelDeletionPost() {
        Optional<Long> userIdOpt = getAuthenticatedUserId();
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        cancelAccountDeletionUseCase.execute(userIdOpt.get());
        return ResponseEntity.ok().build();
    }

    private Optional<Long> getAuthenticatedUserId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(authentication.getName()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private UserProfile mapToUserProfile(User user) {
        UserProfile profile = new UserProfile();
        profile.setId(user.getId());
        profile.setEmail(user.getEmail());
        profile.setFullName(user.getFullName());

        if (user.getInvestorProfile() != null) {
            profile.setInvestorProfile(afsdigital.grahamselect.model.InvestorProfile.fromValue(user.getInvestorProfile().name()));
        }

        var domainTier = subscriptionTierService.resolveEffectiveTier(user, true);

        profile.setTier(SubscriptionTier.fromValue(domainTier.name()));

        if (user.getTrialEndsAt() != null) {
            profile.setTrialEndsAt(user.getTrialEndsAt().atOffset(ZoneOffset.UTC));
            long daysRemaining = Duration.between(LocalDateTime.now(ZoneOffset.UTC), user.getTrialEndsAt()).toDays();
            profile.setDaysRemaining((int) Math.max(0, daysRemaining));
        }

        return profile;
    }
}
