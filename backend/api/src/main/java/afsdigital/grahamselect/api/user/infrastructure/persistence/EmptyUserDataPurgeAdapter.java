package afsdigital.grahamselect.api.user.infrastructure.persistence;

import afsdigital.grahamselect.common.user.application.repository.UserDataPurgePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmptyUserDataPurgeAdapter implements UserDataPurgePort {
    @Override
    public void purgeAllUserData(Long userId) {
        log.info("Purging all user data for user ID: {} (Placeholder implementation)", userId);
    }
}
