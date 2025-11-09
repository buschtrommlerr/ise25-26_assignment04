package de.seuhd.campuscoffee.api.exceptions;

import de.seuhd.campuscoffee.domain.exceptions.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

/**
 * Global exception handler for all controllers.
 * Provides centralized exception handling and standardized error responses.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Handles all "Not Found" exceptions from the domain layer.
     * Returns HTTP 404 (Not Found).
     *
     * @param exception theNotFoundException that was thrown
     * @param request the web request
     * @return ResponseEntity with ErrorResponse and HTTP 404
     */
    @ExceptionHandler({
            PosNotFoundException.class,
            OsmNodeNotFoundException.class
    })
    public ResponseEntity<ErrorResponse> handleNotFoundException(
            RuntimeException exception,
            WebRequest request
    ) {
        log.warn("Resource not found: {}", exception.getMessage());
        return buildErrorResponse(exception, HttpStatus.NOT_FOUND, request);
    }

    /**
     * Handles duplicate/uniqueness constraint violations.
     * Returns HTTP 409 (Conflict) - standard status for resource conflicts.
     *
     * @param exception the duplicate exception that was thrown
     * @param request the web request
     * @return ResponseEntity with ErrorResponse and HTTP 409
     */
    @ExceptionHandler({
            DuplicatePosNameException.class
    })
    public ResponseEntity<ErrorResponse> handleDuplicateException(
            RuntimeException exception,
            WebRequest request
    ) {
        log.warn("Duplicate resource: {}", exception.getMessage());
        return buildErrorResponse(exception, HttpStatus.CONFLICT, request);
    }

    /**
     * Handles validation and bad request exceptions.
     * Returns HTTP 400 (Bad Request).
     *
     * @param exception the exception that was thrown
     * @param request the web request
     * @return ResponseEntity with ErrorResponse and HTTP 400
     */
    @ExceptionHandler({
            IllegalArgumentException.class,
            OsmNodeMissingFieldsException.class
    })
    public ResponseEntity<ErrorResponse> handleBadRequestException(
            RuntimeException exception,
            WebRequest request
    ) {
        log.warn("Bad request: {}", exception.getMessage());
        return buildErrorResponse(exception, HttpStatus.BAD_REQUEST, request);
    }

    /**
     * Handles upstream errors from external OSM service.
     * Returns HTTP 502 (Bad Gateway).
     *
     * @param exception the upstream exception that was thrown
     * @param request the web request
     * @return ResponseEntity with ErrorResponse and HTTP 502
     */
    @ExceptionHandler({
            OsmUpstreamException.class
    })
    public ResponseEntity<ErrorResponse> handleUpstreamException(
            RuntimeException exception,
            WebRequest request
    ) {
        log.warn("Upstream error: {}", exception.getMessage());
        return buildErrorResponse(exception, HttpStatus.BAD_GATEWAY, request);
    }

    /**
     * Fallback handler for unexpected exceptions.
     * Returns HTTP 500 (Internal Server Error).
     *
     * @param exception the unexpected exception that was thrown
     * @param request the web request
     * @return ResponseEntity with ErrorResponse and HTTP 500
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception exception,
            WebRequest request
    ) {
        log.error("Unexpected error occurred", exception);
        return buildErrorResponse(exception, HttpStatus.INTERNAL_SERVER_ERROR, request,
                "An unexpected error occurred.");
    }

    /**
     * Builds a standardized error response using the exception message.
     *
     * @param exception the exception that was thrown
     * @param status the HTTP status to return
     * @param request the web request
     * @return ResponseEntity with ErrorResponse and the specified HTTP status
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception exception,
            HttpStatus status,
            WebRequest request
    ) {
        return buildErrorResponse(exception, status, request, exception.getMessage());
    }

    /**
     * Builds a standardized error response with a custom message.
     *
     * @param exception the exception that was thrown
     * @param status the HTTP status to return
     * @param request the web request
     * @param message custom error message (overrides exception message).
     * @return ResponseEntity with ErrorResponse and the specified HTTP status
     */
    private ResponseEntity<ErrorResponse> buildErrorResponse(
            Exception exception,
            HttpStatus status,
            WebRequest request,
            String message
    ) {
        ErrorResponse error = ErrorResponse.builder()
                .errorCode(exception.getClass().getSimpleName())
                .message(message)
                .statusCode(status.value())
                .statusMessage(status.getReasonPhrase())
                .timestamp(LocalDateTime.now())
                .path(extractPath(request))
                .build();

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Extracts the request path from the WebRequest.
     *
     * @param request the web request
     * @return the request URI or "unknown" if not available
     */
    private String extractPath(WebRequest request) {
        if (request instanceof ServletWebRequest) {
            HttpServletRequest servletRequest = ((ServletWebRequest) request).getRequest();
            return servletRequest.getRequestURI();
        }
        return "unknown";
    }
}
