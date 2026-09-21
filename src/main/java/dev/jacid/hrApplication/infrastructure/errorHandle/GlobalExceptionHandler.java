package dev.jacid.hrApplication.infrastructure.errorHandle;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import dev.jacid.hrApplication.adapter.in.http.dto.ErrorResponse;
import dev.jacid.hrApplication.domain.exception.EmployeeAlreadyExistsException;
import dev.jacid.hrApplication.domain.exception.EmployeeNotFoundException;
import dev.jacid.hrApplication.domain.exception.InvalidEmployeeDataException;
import dev.jacid.hrApplication.domain.exception.InvalidFeedbackException;
import dev.jacid.hrApplication.domain.exception.InvalidRequestException;
import dev.jacid.hrApplication.domain.exception.OperationNotAllowedException;

/**
 * Translates domain exceptions and malformed requests into HTTP responses with an {@link ErrorResponse} body.
 * Authorization failures from {@code @PreAuthorize} are left to Spring Security (401/403).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({EmployeeNotFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(Exception ex) {
        return error(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler({InvalidEmployeeDataException.class, InvalidFeedbackException.class, InvalidRequestException.class})
    public ResponseEntity<ErrorResponse> handleBadRequest(RuntimeException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** E.g. a path or query parameter that is not a UUID or not a number. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return error(HttpStatus.BAD_REQUEST, "Invalid value for '" + ex.getName() + "'");
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex) {
        return error(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException ex) {
        return error(HttpStatus.BAD_REQUEST, "Malformed JSON request body");
    }

    @ExceptionHandler(EmployeeAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleConflict(EmployeeAlreadyExistsException ex) {
        return error(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(OperationNotAllowedException ex) {
        return error(HttpStatus.FORBIDDEN, ex.getMessage());
    }

    private static ResponseEntity<ErrorResponse> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(status.name(), message));
    }
}
