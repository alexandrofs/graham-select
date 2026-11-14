package afsdigital.grahamselect.api.common.web;

import afsdigital.grahamselect.common.family.application.service.exceptions.DuplicateFamilyProfileException;
import afsdigital.grahamselect.common.family.application.service.exceptions.FamilyProfileNotFoundException;
import afsdigital.grahamselect.common.family.application.service.exceptions.InvalidCpfException;
import afsdigital.grahamselect.common.user.application.service.exceptions.DeletionAlreadyPendingException;
import afsdigital.grahamselect.common.user.application.service.exceptions.DeletionNotFoundException;
import afsdigital.grahamselect.api.upload.web.B3UploadValidationException;
import afsdigital.grahamselect.goals.application.service.exceptions.FinancialGoalNotFoundException;
import afsdigital.grahamselect.valuation.application.usecase.exceptions.AllocationGoalValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.stream.Collectors;

/**
 * Global exception handler providing RFC 7807 ProblemDetail responses.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AllocationGoalValidationException.class)
    public ProblemDetail handleAllocationGoalValidation(AllocationGoalValidationException ex) {
        log.error("Allocation goal validation failed: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Allocation Goal Validation Error");
        return problem;
    }

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
        log.error("Deletion not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Deletion Not Found");
        return problem;
    }

    @ExceptionHandler(afsdigital.grahamselect.api.dlq.application.service.exceptions.DlqMessageNotFoundException.class)
    public ProblemDetail handleDlqMessageNotFound(afsdigital.grahamselect.api.dlq.application.service.exceptions.DlqMessageNotFoundException ex) {
        log.error("DLQ message not found: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("DLQ Message Not Found");
        return problem;
    }

    @ExceptionHandler(FinancialGoalNotFoundException.class)
    public ProblemDetail handleFinancialGoalNotFound(FinancialGoalNotFoundException ex) {
        log.error("Financial goal not found or access denied: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Financial Goal Not Found");
        return problem;
    }

    @ExceptionHandler(afsdigital.grahamselect.api.admin.application.service.exceptions.DuplicateResourceException.class)
    public ProblemDetail handleDuplicateResource(afsdigital.grahamselect.api.admin.application.service.exceptions.DuplicateResourceException ex) {
        log.error("Duplicate resource error: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Resource");
        return problem;
    }

    @ExceptionHandler(afsdigital.grahamselect.api.admin.application.service.exceptions.ResourceNotFoundException.class)
    public ProblemDetail handleResourceNotFound(afsdigital.grahamselect.api.admin.application.service.exceptions.ResourceNotFoundException ex) {
        log.error("Resource not found error: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        return problem;
    }

    @ExceptionHandler(InvalidCpfException.class)
    public ProblemDetail handleInvalidCpf(InvalidCpfException ex) {
        log.error("Invalid CPF provided: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid CPF");
        return problem;
    }

    @ExceptionHandler(DuplicateFamilyProfileException.class)
    public ProblemDetail handleDuplicateFamilyProfile(DuplicateFamilyProfileException ex) {
        log.error("Duplicate family profile: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Family Profile");
        return problem;
    }

    @ExceptionHandler(FamilyProfileNotFoundException.class)
    public ProblemDetail handleFamilyProfileNotFound(FamilyProfileNotFoundException ex) {
        log.error("Family profile not found: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Family Profile Not Found");
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        log.error("Invalid argument or business rule violation: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Bad Request");
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        if (detail.isBlank()) {
            detail = "Dados de entrada inválidos.";
        }
        log.error("Request validation failed: {}", detail, ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation Error");
        return problem;
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        log.error("Database integrity violation: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "Operação bloqueada por conflito ou vínculo de integridade referencial."
        );
        problem.setTitle("Data Integrity Violation");
        return problem;
    }
}
