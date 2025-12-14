package ru.practicum.service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class StatsUnavailableException extends StatsServiceException {
    public StatsUnavailableException(String message) {
        super(message);
    }

    public StatsUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}