package ru.practicum.client.exception;

import java.io.Serial;

public class StatsClientBadRequestException extends StatsClientException {
    @Serial
    private static final long serialVersionUID = 1L;
    public StatsClientBadRequestException(String message) { super(message); }
}