package ru.practicum.ewm.event.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.category.service.CategoryService;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.UpdateEventAdminRequest;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.model.StateAction;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventAdminServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private ParticipationRequestRepository requestRepository;

    @Mock
    private CategoryService categoryService;


    @Mock
    private EventMapper mapper;

    @InjectMocks
    private EventAdminService eventAdminService;

    private Event event;
    private User user;
    private Category category;
    private Location location;

    @BeforeEach
    void setUp() {
        user = new User(1L,"Test User","test@example.com");
        category = new Category(1L,"Test Category");
        location = new Location(1L,55.7558f,37.6176f);
        event = new Event();
        event.setId(1L);
        event.setTitle("Test Event");
        event.setAnnotation("Test annotation");
        event.setDescription("Test description");
        event.setEventDate(LocalDateTime.now().plusDays(1));
        event.setCreatedOn(LocalDateTime.now());
        event.setInitiator(user);
        event.setCategory(category);
        event.setLocation(location);
        event.setState(EventState.PENDING);
        event.setPaid(false);
        event.setParticipantLimit(0);
        event.setRequestModeration(true);
    }

    @Test
    void updateEvent_ValidRequest_ShouldReturnUpdatedEvent() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setTitle("Updated Title");
        request.setEventDate(LocalDateTime.now().plusDays(2));

        EventFullDto expectedDto = new EventFullDto();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(0L);
        when(mapper.toEventFullDto(any(Event.class), anyLong(), anyLong())).thenReturn(expectedDto);

        EventFullDto result = eventAdminService.updateEvent(1L, request);

        assertNotNull(result);
        verify(eventRepository).save(event);
        assertEquals("Updated Title", event.getTitle());
    }

    @Test
    void updateEvent_EventNotFound_ShouldThrowNotFoundException() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();

        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> eventAdminService.updateEvent(1L, request));

        assertEquals("Событие с ID 1 не найдено", exception.getMessage());
    }

    @Test
    void updateEvent_WithInvalidEventDate_ShouldThrowBadRequestException() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setEventDate(LocalDateTime.now().minusHours(1));

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> eventAdminService.updateEvent(1L, request));

        assertEquals("Дата начала изменяемого события должна быть не ранее, чем за час от даты публикации",
                exception.getMessage());
    }

    @Test
    void updateEvent_PublishEvent_ShouldChangeStateToPublished() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(StateAction.PUBLISH_EVENT);
        event.setEventDate(LocalDateTime.now().plusHours(2));

        EventFullDto expectedDto = new EventFullDto();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(0L);
        when(mapper.toEventFullDto(any(Event.class), anyLong(), anyLong())).thenReturn(expectedDto);

        EventFullDto result = eventAdminService.updateEvent(1L, request);

        assertNotNull(result);
        assertEquals(EventState.PUBLISHED, event.getState());
        assertNotNull(event.getPublishedOn());
    }

    @Test
    void updateEvent_PublishAlreadyPublishedEvent_ShouldThrowConflictException() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(StateAction.PUBLISH_EVENT);
        event.setState(EventState.PUBLISHED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventAdminService.updateEvent(1L, request));

        assertEquals("Событие можно публиковать только если оно в состоянии ожидания модерации",
                exception.getMessage());
    }

    @Test
    void updateEvent_RejectEvent_ShouldChangeStateToCanceled() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(StateAction.REJECT_EVENT);

        EventFullDto expectedDto = new EventFullDto();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(0L);
        when(mapper.toEventFullDto(any(Event.class), anyLong(), anyLong())).thenReturn(expectedDto);

        EventFullDto result = eventAdminService.updateEvent(1L, request);

        assertNotNull(result);
        assertEquals(EventState.CANCELED, event.getState());
    }

    @Test
    void updateEvent_RejectPublishedEvent_ShouldThrowConflictException() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setStateAction(StateAction.REJECT_EVENT);
        event.setState(EventState.PUBLISHED);

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> eventAdminService.updateEvent(1L, request));

        assertEquals("Событие можно отклонить только если оно еще не опубликовано",
                exception.getMessage());
    }

    @Test
    void updateEvent_WithCategoryUpdate_ShouldUpdateCategory() {
        UpdateEventAdminRequest request = new UpdateEventAdminRequest();
        request.setCategory(2L);

        Category newCategory = new Category(2L,"New Category");
        EventFullDto expectedDto = new EventFullDto();

        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(categoryService.getCategoryById(2L)).thenReturn(newCategory);
        when(eventRepository.save(any(Event.class))).thenReturn(event);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(0L);
        when(mapper.toEventFullDto(any(Event.class), anyLong(), anyLong())).thenReturn(expectedDto);

        EventFullDto result = eventAdminService.updateEvent(1L, request);

        assertNotNull(result);
        assertEquals(newCategory, event.getCategory());
        verify(categoryService).getCategoryById(2L);
    }
}