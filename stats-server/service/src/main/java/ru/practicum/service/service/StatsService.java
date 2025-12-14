package ru.practicum.service.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.service.exception.InvalidStatsRequestException;
import ru.practicum.service.exception.StatsUnavailableException;
import ru.practicum.service.model.EndpointHit;
import ru.practicum.service.repository.EndpointHitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsService {

    private final EndpointHitRepository repository;

    @Transactional
    public void saveHit(EndpointHitDto dto) {
        // на случай, если DTO пришёл без валидации
        if (dto == null) {
            throw new InvalidStatsRequestException("Некорректный запрос");
        }
        if (dto.getApp() == null || dto.getApp().isBlank()) {
            throw new InvalidStatsRequestException("Поле 'app' не должно быть пустым");
        }
        if (dto.getUri() == null || dto.getUri().isBlank()) {
            throw new InvalidStatsRequestException("Поле 'uri' не должно быть пустым");
        }
        if (dto.getIp() == null || dto.getIp().isBlank()) {
            throw new InvalidStatsRequestException("Поле 'ip' не должно быть пустым");
        }
        if (dto.getTimestamp() == null) {
            throw new InvalidStatsRequestException("Поле 'timestamp' не должно быть пустым");
        }

        if (dto.getTimestamp().isAfter(LocalDateTime.now())) {
            throw new InvalidStatsRequestException("Время запроса не может быть в будущем");
        }

        EndpointHit hit = EndpointHit.builder()
                .app(dto.getApp())
                .uri(dto.getUri())
                .ip(dto.getIp())
                .timestamp(dto.getTimestamp())
                .build();
        try {
            repository.save(hit);
        } catch (DataAccessException dae) {
            throw new StatsUnavailableException("Ошибка при сохранении", dae);
        }
    }

    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique) {
        // проверка временных границ
        if (start == null || end == null) {
            throw new InvalidStatsRequestException("Дата начала и окончания должны быть указаны");
        }
        if (start.isAfter(end)) {
            throw new InvalidStatsRequestException("Дата начала должна быть раньше даты окончания");
        }

        try {
            if (unique) {
                return repository.findStatsUnique(start, end, uris == null || uris.isEmpty() ? null : uris);
            }
            return repository.findStats(start, end, uris == null || uris.isEmpty() ? null : uris);
        } catch (DataAccessException dae) {
            throw new StatsUnavailableException("Ошибка при сохранении", dae);
        }
    }
}
