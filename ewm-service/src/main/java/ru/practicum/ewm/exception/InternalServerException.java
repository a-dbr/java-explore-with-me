package ru.practicum.ewm.exception;

import org.springframework.http.HttpStatus;

public class InternalServerException extends ApiException {
    public InternalServerException(String message) { super(message); }
    @Override public HttpStatus getStatus() { return HttpStatus.INTERNAL_SERVER_ERROR; }
}
