package afsdigital.grahamselect.common.notifications.application.exceptions;

public class WebPushSubscriptionNotFoundException extends RuntimeException {
    public WebPushSubscriptionNotFoundException(String message) {
        super(message);
    }
}
