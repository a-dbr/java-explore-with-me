package ru.practicum.ewm.request.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.request.mapper.RequestMapper;
import ru.practicum.ewm.request.dto.ParticipationRequestDto;
import ru.practicum.ewm.request.model.ParticipationRequest;
import ru.practicum.ewm.request.model.RequestStatus;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestPrivateServiceTest {

    @Mock
    private ParticipationRequestRepository requestRepository;

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RequestMapper mapper;

    @InjectMocks
    private RequestPrivateService requestPrivateService;

    private User user;
    private User initiator;
    private Event event;
    private ParticipationRequest request;
    private ParticipationRequestDto requestDto;
    private Category category;
    private Location location;

    @BeforeEach
    void setUp() {
        user = new User(1L,"Test User","test@example.com");
        initiator = new User(2L,"Event Initiator","initiator@example.com");
        category = new Category(1L,"Test Category");
        location = new Location(1L,55.7558f,37.6176f);

        event = new Event();
        event.setId(1L);
        event.setTitle("Test Event");
        event.setAnnotation("Test annotation");
        event.setDescription("Test description");
        event.setCategory(category);
        event.setInitiator(initiator);
        event.setLocation(location);
        event.setEventDate(LocalDateTime.now().plusDays(1));
        event.setState(EventState.PUBLISHED);
        event.setPaid(false);
        event.setParticipantLimit(10);
        event.setRequestModeration(true);

        request = new ParticipationRequest(1L,LocalDateTime.now(),event,user,RequestStatus.PENDING);
        requestDto = new ParticipationRequestDto(1L,1L,1L,LocalDateTime.now(),"PENDING");
    }

    @Test
    void getUserRequests_ShouldReturnUserRequests() {
        List<ParticipationRequest> requests = Collections.singletonList(request);
        when(requestRepository.findAllByRequesterId(1L)).thenReturn(requests);
        when(mapper.toParticipationRequestDto(request)).thenReturn(requestDto);

        List<ParticipationRequestDto> result = requestPrivateService.getUserRequests(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(requestDto, result.getFirst());
        verify(requestRepository).findAllByRequesterId(1L);
        verify(mapper).toParticipationRequestDto(request);
    }

    @Test
    void addRequest_ValidRequest_ShouldCreateRequest() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(false);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(5L);
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);
        when(mapper.toParticipationRequestDto(request)).thenReturn(requestDto);

        ParticipationRequestDto result = requestPrivateService.addRequest(1L, 1L);

        assertNotNull(result);
        assertEquals(requestDto, result);
        verify(requestRepository).save(any(ParticipationRequest.class));
    }

    @Test
    void addRequest_UserIsInitiator_ShouldThrowConflictException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(initiator));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestPrivateService.addRequest(2L, 1L));

        assertEquals("Нельзя добавить запрос на участие в своём событии", exception.getMessage());
    }

    @Test
    void addRequest_EventNotPublished_ShouldThrowConflictException() {
        event.setState(EventState.PENDING);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));

        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestPrivateService.addRequest(1L, 1L));

        assertEquals("Нельзя участвовать в неопубликованном событии", exception.getMessage());
    }

    @Test
    void addRequest_RequestAlreadyExists_ShouldThrowConflictException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(true);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestPrivateService.addRequest(1L, 1L));

        assertEquals("Запрос уже существует", exception.getMessage());
    }

    @Test
    void addRequest_ParticipantLimitReached_ShouldThrowConflictException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(false);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(10L);

        ConflictException exception = assertThrows(ConflictException.class,
                () -> requestPrivateService.addRequest(1L, 1L));

        assertEquals("Лимит участников исчерпан", exception.getMessage());
    }

    @Test
    void addRequest_NoModerationRequired_ShouldAutoConfirm() {
        event.setRequestModeration(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(false);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(5L);
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);
        when(mapper.toParticipationRequestDto(request)).thenReturn(requestDto);

        ParticipationRequestDto result = requestPrivateService.addRequest(1L, 1L);

        assertNotNull(result);
        verify(requestRepository).save(argThat(req -> req.getStatus() == RequestStatus.CONFIRMED));
    }

    @Test
    void addRequest_NoParticipantLimit_ShouldAutoConfirm() {
        event.setParticipantLimit(0);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(event));
        when(requestRepository.existsByRequesterIdAndEventId(1L, 1L)).thenReturn(false);
        when(requestRepository.countConfirmedRequestsByEventId(1L)).thenReturn(5L);
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);
        when(mapper.toParticipationRequestDto(request)).thenReturn(requestDto);

        ParticipationRequestDto result = requestPrivateService.addRequest(1L, 1L);

        assertNotNull(result);
        verify(requestRepository).save(argThat(req -> req.getStatus() == RequestStatus.CONFIRMED));
    }

    @Test
    void addRequest_UserNotFound_ShouldThrowNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestPrivateService.addRequest(1L, 1L));

        assertEquals("Пользователь с id=1 не найден", exception.getMessage());
    }

    @Test
    void addRequest_EventNotFound_ShouldThrowNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> requestPrivateService.addRequest(1L, 1L));

        assertEquals("Событие с id=1 не найдено", exception.getMessage());
    }

    @Test
    void cancelRequest_ValidRequest_ShouldCancelRequest() {
        when(requestRepository.findById(1L)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(ParticipationRequest.class))).thenReturn(request);
        when(mapper.toParticipationRequestDto(request)).thenReturn(requestDto);

        ParticipationRequestDto result = requestPrivateService.cancelRequest(1L, 1L);

        assertNotNull(result);
        verify(requestRepository).save(argThat(req -> req.getStatus() == RequestStatus.CANCELED));
    }
}