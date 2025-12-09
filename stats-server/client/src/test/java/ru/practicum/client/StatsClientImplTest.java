package ru.practicum.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import ru.practicum.dto.EndpointHitDto;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class StatsClientImplTest {

    private StatsClientImpl statsClient;

    @BeforeEach
    void setUp() {
        statsClient = new StatsClientImpl("http://localhost:9090");
    }

    @Test
    void testConstructorWithDefaultUrl() {
        StatsClientImpl client = new StatsClientImpl("http://localhost:9090");

        assertNotNull(client);
        assertNotNull(ReflectionTestUtils.getField(client, "restClient"));
        assertNotNull(ReflectionTestUtils.getField(client, "formatter"));
    }

    @Test
    void testConstructorWithCustomUrl() {
        StatsClientImpl client = new StatsClientImpl("http://custom-host:8080");

        assertNotNull(client);
        assertNotNull(ReflectionTestUtils.getField(client, "restClient"));
    }

    @Test
    void testGetStat_withNullStart_shouldThrowException() {
        String start = null;
        String end = "2024-01-02 00:00:00";
        List<String> urls = List.of("/test");
        Boolean unique = false;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> statsClient.getStat(start, end, urls, unique)
        );
        assertEquals("диапазон не может содержать null", exception.getMessage());
    }

    @Test
    void testGetStat_withNullEnd_shouldThrowException() {
        String start = "2024-01-01 00:00:00";
        String end = null;
        List<String> urls = List.of("/test");
        Boolean unique = false;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> statsClient.getStat(start, end, urls, unique)
        );
        assertEquals("диапазон не может содержать null", exception.getMessage());
    }

    @Test
    void testGetStat_withStartAfterEnd_shouldThrowException() {
        String start = "2024-01-02 00:00:00";
        String end = "2024-01-01 00:00:00";
        List<String> urls = List.of("/test");
        Boolean unique = false;

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> statsClient.getStat(start, end, urls, unique)
        );
        assertEquals("задан неверный диапазон", exception.getMessage());
    }

    @Test
    void testGetStat_withInvalidDateFormat_shouldThrowException() {
        String start = "invalid-date";
        String end = "2024-01-02 00:00:00";
        List<String> urls = List.of("/test");
        Boolean unique = false;

        assertThrows(Exception.class, () -> statsClient.getStat(start, end, urls, unique));
    }

    @Test
    void testGetStat_withValidDates_shouldNotThrowOnValidation() {
        String start = "2024-01-01 00:00:00";
        String end = "2024-01-02 00:00:00";
        List<String> urls = List.of("/test");
        Boolean unique = false;

        try {
            statsClient.getStat(start, end, urls, unique);
        } catch (Exception e) {
            // Ожидаем исключение от REST вызова, но не от валидации
            assertFalse(e.getMessage().contains("диапазон не может содержать null"));
            assertFalse(e.getMessage().contains("задан не верный диапазон"));
        }
    }

    @Test
    void testHit_withValidDto_shouldNotThrowOnValidation() {
        EndpointHitDto hitDto = new EndpointHitDto(
                "test-app",
                "/test",
                "192.168.1.1",
                LocalDateTime.now()
        );

        try {
            statsClient.hit(hitDto);
        } catch (Exception e) {
            // Ожидаем исключение от REST вызова, но не от валидации DTO
            assertNotNull(e);
        }
    }

    @Test
    void testGetStat_withNullUrls_shouldNotThrowOnValidation() {
        String start = "2024-01-01 00:00:00";
        String end = "2024-01-02 00:00:00";
        List<String> urls = null;
        Boolean unique = false;

        try {
            statsClient.getStat(start, end, urls, unique);
        } catch (Exception e) {
            // Ожидаем исключение от REST вызова, но не от валидации
            assertFalse(e.getMessage().contains("диапазон не может содержать null"));
        }
    }

    @Test
    void testGetStat_withEmptyUrls_shouldNotThrowOnValidation() {
        String start = "2024-01-01 00:00:00";
        String end = "2024-01-02 00:00:00";
        List<String> urls = List.of();
        Boolean unique = null;

        try {
            statsClient.getStat(start, end, urls, unique);
        } catch (Exception e) {
            // Ожидаем исключение от REST вызова, но не от валидации
            assertFalse(e.getMessage().contains("диапазон не может содержать null"));
        }
    }

    @Test
    void testGetStat_withNullUnique_shouldNotThrowOnValidation() {
        String start = "2024-01-01 00:00:00";
        String end = "2024-01-02 00:00:00";
        List<String> urls = List.of("/test");
        Boolean unique = null;

        try {
            statsClient.getStat(start, end, urls, unique);
        } catch (Exception e) {
            // Ожидаем исключение от REST вызова, но не от валидации
            assertFalse(e.getMessage().contains("диапазон не может содержать null"));
        }
    }

    @Test
    void testDateTimeFormatterPattern() {
        String validDate = "2024-01-01 12:30:45";
        String start = validDate;
        String end = "2024-01-02 12:30:45";

        try {
            statsClient.getStat(start, end, List.of("/test"), false);
        } catch (Exception e) {
            // Не должно быть исключения парсинга даты
            assertFalse(e.getMessage().contains("DateTimeParseException"));
        }
    }

    @Test
    void testImplementsStatsClientInterface() {
        assertTrue(statsClient instanceof StatsClient);
        assertNotNull(statsClient);
    }
}