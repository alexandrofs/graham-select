package afsdigital.grahamselect.common.user.application.usecase;

import afsdigital.grahamselect.common.user.domain.entities.InvestorProfile;
import afsdigital.grahamselect.common.user.domain.entities.User;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@RequiredArgsConstructor
@Slf4j
public class UpdateInvestorProfileUseCase {

    private final UserRepository userRepository;

    public User execute(Long userId, InvestorProfile newProfile) {
        log.info("Updating investor profile for user {} to {}", userId, newProfile);
        
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }
        
        User user = userOptional.get();
        if (newProfile != null) {
            user.setInvestorProfile(newProfile);
        }
        
        return userRepository.save(user);
    }
}
