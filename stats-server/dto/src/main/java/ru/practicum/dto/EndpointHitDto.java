package ru.practicum.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EndpointHitDto {

    @NotBlank(message = "Идентификатор сервиса не должен быть пустым")
    private String app;

    @NotBlank(message = "URI не должен быть пустым")
    private String uri;

    @NotBlank(message = "IP-адрес не должен быть пустым")
    private String ip;

    @NotNull(message = "Время запроса не должно быть пустым")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime timestamp;
}
