package ru.practicum.ewm.event.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventSearchParams;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.service.EventPublicService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PublicEventController.class)
class PublicEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EventPublicService eventPublicService;

    @Test
    void getEvents_WithDefaultParameters_ShouldReturnEventList() throws Exception {
        List<EventShortDto> events = List.of(
                new EventShortDto(),
                new EventShortDto()
        );

        when(eventPublicService.getEvents(any(EventSearchParams.class), anyString(), anyString()))
                .thenReturn(events);

        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getEvents_WithParameters_ShouldReturnFilteredEvents() throws Exception {
        List<EventShortDto> events = List.of(new EventShortDto());

        when(eventPublicService.getEvents(any(EventSearchParams.class), anyString(), anyString()))
                .thenReturn(events);

        mockMvc.perform(get("/events")
                        .param("text", "test")
                        .param("categories", "1,2")
                        .param("paid", "true")
                        .param("from", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void getEventById_ValidId_ShouldReturnEvent() throws Exception {
        EventFullDto event = new EventFullDto();

        when(eventPublicService.getEventById(eq(1L), anyString(), anyString()))
                .thenReturn(event);

        mockMvc.perform(get("/events/1"))
                .andExpect(status().isOk());
    }

    @Test
    void getEvents_WithInvalidDateRange_ShouldReturnBadRequest() throws Exception {
        String start = LocalDateTime.now().plusDays(2)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String end = LocalDateTime.now().plusDays(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        mockMvc.perform(get("/events")
                        .param("rangeStart", start)
                        .param("rangeEnd", end))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Дата начала не может быть позже даты окончания"));

        verify(eventPublicService, never()).getEvents(any(), anyString(), anyString());
    }
}
