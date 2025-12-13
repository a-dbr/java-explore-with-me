package ru.practicum.client.exception;

import java.io.Serial;

public class StatsClientUnavailableException extends StatsClientException {
    @Serial
    private static final long serialVersionUID = 1L;

    public StatsClientUnavailableException(String message) {
        super(message);
    }
}