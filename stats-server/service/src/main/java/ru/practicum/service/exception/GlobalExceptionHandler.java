package ru.practicum.service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.dto.ApiErrorDto;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private ApiErrorDto build(HttpStatus status, String reason, String message, List<String> errors) {
        return new ApiErrorDto(
                status.name(),
                reason,
                message,
                errors,
                LocalDateTime.now().format(TS_FMT)
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorDto> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .toList();

        log.warn("Validation error: {}", details);
        return ResponseEntity.badRequest().body(
                build(HttpStatus.BAD_REQUEST, "Ошибка валидации",
                        "Переданы некорректные поля", details)
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorDto> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();
        String reason = status.getReasonPhrase();
        String message = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        log.warn("ResponseStatusException: {} - {}", status, message, ex);
        return ResponseEntity.status(status).body(
                build(status, reason, message, List.of(ex.getClass().getSimpleName()))
        );
    }

    @ExceptionHandler({
            InvalidStatsRequestException.class,
            IllegalArgumentException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class,
            DateTimeParseException.class
    })
    public ResponseEntity<ApiErrorDto> handleBadRequest(Exception ex) {
        log.warn("Bad request: {}", ex.getMessage(), ex);
        return ResponseEntity.badRequest().body(
                build(HttpStatus.BAD_REQUEST, "Некорректный запрос",
                        ex.getMessage(), List.of(ex.getClass().getSimpleName()))
        );
    }

    @ExceptionHandler({DataAccessException.class, StatsUnavailableException.class})
    public ResponseEntity<ApiErrorDto> handleUnavailable(Exception ex) {
        log.error("Service unavailable", ex);
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(
                build(HttpStatus.SERVICE_UNAVAILABLE, "Сервис временно недоступен",
                        "Попробуйте повторить запрос позже", List.of(ex.getClass().getSimpleName()))
        );
    }

    @ExceptionHandler(StatsServiceException.class)
    public ResponseEntity<ApiErrorDto> handleStatsServiceException(StatsServiceException ex) {
        log.error("Stats service error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                build(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервиса",
                        "Произошла ошибка при обработке запроса", List.of(ex.getClass().getSimpleName()))
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorDto> handleAny(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                build(HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера",
                        "Произошла непредвиденная ошибка", List.of(ex.getClass().getSimpleName()))
        );
    }
}
