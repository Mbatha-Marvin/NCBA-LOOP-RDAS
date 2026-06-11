package com.ncba.loop.exception;

import com.ncba.loop.dto.ApiErrorResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.Collections;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, WebRequest request) {
        return buildResponse(request, HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleServiceUnavailable(ServiceUnavailableException ex, WebRequest request) {
        log.error("Service unavailable", ex);
        return buildResponse(request, HttpStatus.SERVICE_UNAVAILABLE, "Service Unavailable",
                "The requested data is temporarily unavailable. Please try again later.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, WebRequest request) {
        return buildResponse(request, HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        var errors = ex.getConstraintViolations().stream()
                .map(v -> new ApiErrorResponse.ValidationError(
                        v.getPropertyPath().toString().replaceAll(".*\\.", ""),
                        v.getMessage()))
                .toList();
        return buildResponse(request, HttpStatus.BAD_REQUEST, "Validation Failed",
                "One or more request parameters are invalid", errors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        return buildResponse(request, HttpStatus.BAD_REQUEST, "Bad Request",
                "Invalid value for parameter '" + ex.getName() + "': " + ex.getValue());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, WebRequest request) {
        return buildResponse(request, HttpStatus.BAD_REQUEST, "Bad Request",
                "Required parameter '" + ex.getParameterName() + "' is missing");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMalformedBody(HttpMessageNotReadableException ex, WebRequest request) {
        return buildResponse(request, HttpStatus.BAD_REQUEST, "Bad Request", "Malformed request body");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex, WebRequest request) {
        log.error("Unexpected error", ex);
        return buildResponse(request, HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "An unexpected error occurred. Please try again later.");
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(WebRequest request, HttpStatus status, String error, String message) {
        return buildResponse(request, status, error, message, Collections.emptyList());
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(WebRequest request, HttpStatus status, String error, String message,
                                                            java.util.List<ApiErrorResponse.ValidationError> validationErrors) {
        String path = request.getDescription(false);
        if (path != null && path.startsWith("uri=")) {
            path = path.substring(4);
        }
        ApiErrorResponse body = ApiErrorResponse.builder()
                .status(status.value())
                .error(error)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .validationErrors(validationErrors)
                .build();
        return ResponseEntity.status(status).body(body);
    }
}
