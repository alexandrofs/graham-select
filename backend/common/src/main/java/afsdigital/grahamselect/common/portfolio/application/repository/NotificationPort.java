package afsdigital.grahamselect.common.portfolio.application.repository;

public interface NotificationPort {
    void sendPortfolioUpdate(String userId);
}
