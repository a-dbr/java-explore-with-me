package ru.practicum.ewm.exception;

public class EmailAlreadyUsedException extends ConflictException {
    public EmailAlreadyUsedException(String email) {
        super("Email " + email + " уже используется");
    }
}
