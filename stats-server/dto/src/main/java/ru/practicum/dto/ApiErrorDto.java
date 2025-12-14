package ru.practicum.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorDto {

    private String status;

    private String reason;

    private String message;

    private List<String> errors;

    private String timestamp;
}