package afsdigital.grahamselect.api.user.infrastructure.persistence;

import afsdigital.grahamselect.common.user.application.repository.UserDeletionPort;
import afsdigital.grahamselect.common.user.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserDeletionAdapter implements UserDeletionPort {

    private final UserRepository userRepository;

    @Override
    public void deleteById(Long userId) {
        userRepository.deleteById(userId);
    }
}
