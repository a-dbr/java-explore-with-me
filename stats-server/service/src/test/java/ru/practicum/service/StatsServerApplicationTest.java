package ru.practicum.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
@ActiveProfiles("test")
class StatsServerApplicationTest {

    @Test
    void contextLoads() {
    }

    @Test
    void main_WhenRun_ThenNoExceptions() {
        assertDoesNotThrow(() -> StatsServerApplication.main(new String[]{}));
    }
}