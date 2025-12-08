package ru.practicum.ewm.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Data
@RequiredArgsConstructor
@AllArgsConstructor
public class ApiError {
    private String status;

    private String reason;

    private String message;

    private List<String> errors;

    private String timestamp;

}
