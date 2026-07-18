package afsdigital.grahamselect.common.portfolio.application.repository;

public interface NotificationPort {
    void sendPortfolioUpdate(String userId);
    void sendNotification(String userId, String eventName, Object payload);
}
