package ru.practicum.client.exception;

import java.io.Serial;

public class StatsClientException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = 1L;

    public StatsClientException(String message) { super(message); }
    public StatsClientException(String message, Throwable cause) { super(message, cause); }
}
