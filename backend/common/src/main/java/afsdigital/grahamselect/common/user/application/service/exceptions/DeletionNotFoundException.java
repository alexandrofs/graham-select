package afsdigital.grahamselect.common.user.application.service.exceptions;

public class DeletionNotFoundException extends RuntimeException {
    public DeletionNotFoundException(String message) {
        super(message);
    }
}
