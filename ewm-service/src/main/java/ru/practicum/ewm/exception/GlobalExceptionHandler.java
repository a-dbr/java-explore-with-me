package ru.practicum.ewm.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Value("${app.debug:false}")
    private boolean debug;

    private String now() {
        return LocalDateTime.now().format(FMT);
    }

    private ApiError build(HttpStatus status, String reason, String message, List<String> errors) {
        return new ApiError(status.name(), reason, message, debug ? errors : null, now());
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex) {
        HttpStatus status = ex.getStatus();
        ApiError error = build(status, mapReason(status), ex.getMessage(), List.of(ex.toString()));
        log.warn("ApiException: {} -> {}", status, ex.getMessage());
        return ResponseEntity.status(status).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> "Field: " + fe.getField() + ". Error: "
                        + fe.getDefaultMessage() + ". Value: " + fe.getRejectedValue())
                .collect(Collectors.joining("; "));
        List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::toString).collect(Collectors.toList());
        ApiError err = build(HttpStatus.BAD_REQUEST, mapReason(HttpStatus.BAD_REQUEST), message, errors);
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> handleConstraint(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
                .collect(Collectors.joining("; "));
        ApiError err = build(HttpStatus.BAD_REQUEST, mapReason(HttpStatus.BAD_REQUEST),
                message, List.of(ex.toString()));
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<ApiError> handleTypeMismatch(Exception ex) {
        ApiError err = build(HttpStatus.BAD_REQUEST, mapReason(HttpStatus.BAD_REQUEST), ex.getMessage(),
                List.of(ex.toString()));
        return ResponseEntity.badRequest().body(err);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException ex) {
        ApiError err = build(HttpStatus.CONFLICT, mapReason(HttpStatus.CONFLICT),
                ex.getMostSpecificCause().getMessage(), List.of(ex.toString()));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(err);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingParam(MissingServletRequestParameterException ex) {
        ApiError err = build(HttpStatus.BAD_REQUEST,mapReason(HttpStatus.BAD_REQUEST),
                ex.getMessage(), List.of(ex.toString()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(err);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        log.error("Unhandled exception", ex);
        ApiError err = build(HttpStatus.INTERNAL_SERVER_ERROR, mapReason(HttpStatus.INTERNAL_SERVER_ERROR),
                "Internal server error", List.of(ex.toString()));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(err);
    }

    private String mapReason(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Неверный запрос.";
            case NOT_FOUND -> "Не найдено";
            case CONFLICT -> "Условия для запрошенной операции не выполнены.";
            case FORBIDDEN -> "Доступ запрещен..";
            default -> status.getReasonPhrase();
        };
    }
}
