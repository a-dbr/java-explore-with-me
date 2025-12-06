package ru.practicum.service.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.service.service.StatsService;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StatsControllerTest {

    @Mock
    private StatsService statsService;

    @InjectMocks
    private StatsController statsController;

    @Test
    void saveHit_shouldCallService() {
        EndpointHitDto hitDto = new EndpointHitDto();
        hitDto.setApp("test-app");
        hitDto.setUri("/test");
        hitDto.setIp("127.0.0.1");
        hitDto.setTimestamp(LocalDateTime.now());

        statsController.saveHit(hitDto);

        verify(statsService).saveHit(hitDto);
    }

    @Test
    void getStats_shouldReturnStatsFromService() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        List<String> uris = List.of("/test");
        boolean unique = true;

        ViewStatsDto viewStats = new ViewStatsDto("test-app", "/test", 10L);
        when(statsService.getStats(eq(start), eq(end), eq(uris), eq(unique)))
                .thenReturn(List.of(viewStats));

        List<ViewStatsDto> result = statsController.getStats(start, end, uris, unique);

        assertEquals(1, result.size());
        assertEquals(viewStats, result.getFirst());
    }

    @Test
    void getStats_withEmptyUris_shouldPassEmptyListToService() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        boolean unique = false;

        ViewStatsDto viewStats = new ViewStatsDto("test-app", "/test", 5L);
        when(statsService.getStats(eq(start), eq(end), eq(Collections.emptyList()), eq(unique)))
                .thenReturn(List.of(viewStats));

        List<ViewStatsDto> result = statsController.getStats(start, end, Collections.emptyList(), unique);

        assertEquals(1, result.size());
    }

    @Test
    void getStats_withNullUris_shouldPassNullToService() {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = LocalDateTime.now();
        boolean unique = false;

        ViewStatsDto viewStats = new ViewStatsDto("test-app", "/test", 15L);
        when(statsService.getStats(eq(start), eq(end), eq(null), eq(unique)))
                .thenReturn(List.of(viewStats));

        List<ViewStatsDto> result = statsController.getStats(start, end, null, unique);

        assertEquals(1, result.size());
    }

    @Test
    void getStats_withStartAfterEnd_shouldThrowResponseStatusException() {
        LocalDateTime start = LocalDateTime.now();
        LocalDateTime end = LocalDateTime.now().minusDays(1);
        List<String> uris = List.of("/test");
        boolean unique = false;

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> statsController.getStats(start, end, uris, unique)
        );

        assertEquals("400 BAD_REQUEST \"Дата начала должна быть раньше даты окончания\"",
                exception.getMessage());

        // Проверяем, что сервис не вызывался
        verify(statsService, never()).getStats(any(), any(), any(), anyBoolean());
    }
}
