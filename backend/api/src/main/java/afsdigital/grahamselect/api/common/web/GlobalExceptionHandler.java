package afsdigital.grahamselect.api.common.web;

import afsdigital.grahamselect.common.user.application.service.exceptions.DeletionAlreadyPendingException;
import afsdigital.grahamselect.common.user.application.service.exceptions.DeletionNotFoundException;
import afsdigital.grahamselect.api.upload.web.B3UploadValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler providing RFC 7807 ProblemDetail responses.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(B3UploadValidationException.class)
    public ProblemDetail handleB3UploadValidation(B3UploadValidationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("B3 Upload Validation Error");
        return problem;
    }

    @ExceptionHandler(DeletionAlreadyPendingException.class)
    public ProblemDetail handleDeletionAlreadyPending(DeletionAlreadyPendingException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Deletion Already Pending");
        return problem;
    }

    @ExceptionHandler(DeletionNotFoundException.class)
    public ProblemDetail handleDeletionNotFound(DeletionNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Deletion Not Found");
        return problem;
    }
}
