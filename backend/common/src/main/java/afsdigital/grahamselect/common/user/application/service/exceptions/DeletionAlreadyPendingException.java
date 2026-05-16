package afsdigital.grahamselect.common.user.application.service.exceptions;

public class DeletionAlreadyPendingException extends RuntimeException {
    public DeletionAlreadyPendingException(String message) {
        super(message);
    }
}
