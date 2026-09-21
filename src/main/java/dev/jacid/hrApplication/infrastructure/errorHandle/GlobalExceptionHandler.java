package dev.jacid.hrApplication.infrastructure.errorHandle;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import dev.jacid.hrApplication.domain.exception.EmployeeAlreadyExistsException;
import dev.jacid.hrApplication.domain.exception.EmployeeNotFoundException;
import dev.jacid.hrApplication.domain.exception.InvalidEmployeeDataException;
import dev.jacid.hrApplication.domain.exception.InvalidFeedbackException;
import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;

/**
 * Translates domain exceptions into HTTP responses with an {@link ErrorResponse} body.
 * Authorization failures from {@code @PreAuthorize} are left to Spring Security (401/403).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({EmployeeNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex) {
        return error(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler({InvalidEmployeeDataException.class, InvalidFeedbackException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return error(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(EmployeeAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(EmployeeAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(OperationNotAllowedException ex) {
        return error(HttpStatus.FORBIDDEN, ex);
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, Exception ex) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.name(), ex.getMessage()));
    }
}
