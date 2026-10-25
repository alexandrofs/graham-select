package afsdigital.grahamselect.api.user.web;

import afsdigital.grahamselect.api.UsersApiDelegate;
import afsdigital.grahamselect.api.user.service.SubscriptionTierService;
import afsdigital.grahamselect.common.user.application.usecase.CancelAccountDeletionUseCase;
import afsdigital.grahamselect.common.user.application.usecase.RequestAccountDeletionUseCase;
import afsdigital.grahamselect.common.user.application.usecase.UpdateInvestorProfileUseCase;
import afsdigital.grahamselect.common.user.domain.entities.AccountDeletionRequest;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.domain.entities.UserRole;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import afsdigital.grahamselect.model.DeletionRequestResponse;
import afsdigital.grahamselect.model.DeletionRequestResponseData;
import afsdigital.grahamselect.model.SubscriptionTier;
import afsdigital.grahamselect.model.UpdateProfileRequest;
import afsdigital.grahamselect.model.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserController implements UsersApiDelegate {

    private final UserRepository userRepository;
    private final SubscriptionTierService subscriptionTierService;
    private final RequestAccountDeletionUseCase requestAccountDeletionUseCase;
    private final CancelAccountDeletionUseCase cancelAccountDeletionUseCase;
    private final UpdateInvestorProfileUseCase updateInvestorProfileUseCase;

    @Override
    public ResponseEntity<UserProfile> usersMeGet() {
        Optional<Long> userIdOpt = getAuthenticatedUserId();
        log.info("Received request GET /api/v1/users/me for userId: {}", userIdOpt.orElse(null));
        try {
            return userIdOpt
                    .flatMap(userRepository::findById)
                    .map(this::mapToUserProfile)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(401).build());
        } catch (Exception ex) {
            log.error("Error processing GET /api/v1/users/me for userId: {}", userIdOpt.orElse(null), ex);
            throw ex;
        }
    }

    @Override
    public ResponseEntity<UserProfile> usersProfilePatch(UpdateProfileRequest updateProfileRequest) {
        Optional<Long> userIdOpt = getAuthenticatedUserId();
        log.info("Received request PATCH /api/v1/users/profile for userId: {}", userIdOpt.orElse(null));
        try {
            return userIdOpt
                    .map(userId -> {
                        afsdigital.grahamselect.common.user.domain.entities.InvestorProfile domainProfile = null;
                        if (updateProfileRequest != null && updateProfileRequest.getInvestorProfile() != null) {
                            domainProfile = afsdigital.grahamselect.common.user.domain.entities.InvestorProfile.valueOf(updateProfileRequest.getInvestorProfile().name());
                        }
                        return updateInvestorProfileUseCase.execute(userId, domainProfile);
                    })
                    .map(this::mapToUserProfile)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.status(401).build());
        } catch (Exception ex) {
            log.error("Error processing PATCH /api/v1/users/profile for userId: {}", userIdOpt.orElse(null), ex);
            throw ex;
        }
    }

    @Override
    public ResponseEntity<DeletionRequestResponse> usersMeDelete() {
        Optional<Long> userIdOpt = getAuthenticatedUserId();
        log.info("Received request DELETE /api/v1/users/me for userId: {}", userIdOpt.orElse(null));
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        try {
            AccountDeletionRequest request = requestAccountDeletionUseCase.execute(userIdOpt.get());

            DeletionRequestResponse response = new DeletionRequestResponse();
            DeletionRequestResponseData data = new DeletionRequestResponseData();
            data.setRequestId(request.getId());
            data.setStatus(request.getStatus().name());
            data.setEstimatedCompletionWithin("24h");
            response.setData(data);

            return ResponseEntity.accepted().body(response);
        } catch (Exception ex) {
            log.error("Error processing DELETE /api/v1/users/me for userId: {}", userIdOpt.get(), ex);
            throw ex;
        }
    }

    @Override
    public ResponseEntity<Void> usersMeCancelDeletionPost() {
        Optional<Long> userIdOpt = getAuthenticatedUserId();
        log.info("Received request POST /api/v1/users/me/cancel-deletion for userId: {}", userIdOpt.orElse(null));
        if (userIdOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }

        try {
            cancelAccountDeletionUseCase.execute(userIdOpt.get());
            return ResponseEntity.ok().build();
        } catch (Exception ex) {
            log.error("Error processing POST /api/v1/users/me/cancel-deletion for userId: {}", userIdOpt.get(), ex);
            throw ex;
        }
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

        boolean isAdmin = user.getRole() == UserRole.ADMIN || hasAdminAuthority();
        profile.setRole(isAdmin ? UserProfile.RoleEnum.ADMIN : UserProfile.RoleEnum.USER);

        return profile;
    }

    private boolean hasAdminAuthority() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equalsIgnoreCase(a.getAuthority()) || "ADMIN".equalsIgnoreCase(a.getAuthority()));
    }
}
