package com.ringvax.api;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import com.ringvax.api.dto.ApiError;
import com.ringvax.data.CsvFormatException;
import com.ringvax.engine.cover.ExactSolverLimitException;
import com.ringvax.engine.graph.UnknownPersonException;
import com.ringvax.service.NetworkNotFoundException;

/**
 * Turns exceptions into the shared error format {@code { error, message, details }}:
 * 400 for invalid input, 404 for an unknown network, 413 for oversized uploads,
 * 422 for a valid request that cannot be solved as asked.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CsvFormatException.class)
    ResponseEntity<ApiError> invalidFile(CsvFormatException e) {
        return respond(HttpStatus.BAD_REQUEST, "INVALID_FILE", e.getMessage(), e.issues());
    }

    @ExceptionHandler(UnknownPersonException.class)
    ResponseEntity<ApiError> unknownPerson(UnknownPersonException e) {
        return respond(HttpStatus.BAD_REQUEST, "UNKNOWN_PERSON",
                "No person with id " + e.personId() + " in this network", List.of(e.personId()));
    }

    @ExceptionHandler(NetworkNotFoundException.class)
    ResponseEntity<ApiError> notFound(NetworkNotFoundException e) {
        return respond(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), List.of());
    }

    @ExceptionHandler(ExactSolverLimitException.class)
    ResponseEntity<ApiError> exactLimit(ExactSolverLimitException e) {
        return respond(HttpStatus.UNPROCESSABLE_CONTENT, "EXACT_SOLVER_" + e.kind().name(), e.getMessage(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> invalidBody(MethodArgumentNotValidException e) {
        List<String> details = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                details.isEmpty() ? "The request is not valid" : details.get(0), details);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> invalidParameters(HandlerMethodValidationException e) {
        List<String> details = e.getAllErrors().stream().map(err -> String.valueOf(err.getDefaultMessage())).toList();
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                details.isEmpty() ? "The request is not valid" : details.get(0), details);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException e) {
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "The request body is missing or is not valid JSON", List.of());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    ResponseEntity<ApiError> missingFile(MissingServletRequestPartException e) {
        return respond(HttpStatus.BAD_REQUEST, "MISSING_FILE",
                "The file '" + e.getRequestPartName() + "' is required", List.of(e.getRequestPartName()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> tooBig(MaxUploadSizeExceededException e) {
        return respond(HttpStatus.CONTENT_TOO_LARGE, "FILE_TOO_LARGE",
                "The upload is larger than the server accepts (100 MB per file)", List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> invalidArgument(IllegalArgumentException e) {
        return respond(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", e.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception e) {
        if (e instanceof ErrorResponse spring) {
            // Spring's own errors (unknown path, wrong HTTP method, ...) keep their status.
            HttpStatus status = HttpStatus.valueOf(spring.getStatusCode().value());
            String detail = spring.getBody().getDetail();
            return respond(status, status.name(), detail == null ? status.getReasonPhrase() : detail, List.of());
        }
        log.error("Unexpected error", e);
        return respond(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Something went wrong on the server. Please try again.", List.of());
    }

    private static ResponseEntity<ApiError> respond(HttpStatus status, String code, String message, List<?> details) {
        return ResponseEntity.status(status).body(new ApiError(code, message, details));
    }
}
