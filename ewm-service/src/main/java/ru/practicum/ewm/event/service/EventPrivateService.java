package ru.practicum.ewm.event.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.category.service.CategoryService;
import ru.practicum.ewm.event.dto.*;
import ru.practicum.ewm.event.mapper.EventMapper;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.model.EventState;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.BadRequestException;
import ru.practicum.ewm.exception.ConflictException;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.location.service.LocationService;
import ru.practicum.ewm.request.repository.ParticipationRequestRepository;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventPrivateService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryService categoryService;
    private final LocationService locationService;
    private final ParticipationRequestRepository requestRepository;
    private final EventMapper mapper;

    @Transactional
    public EventFullDto createEvent(Long userId, NewEventDto dto) {

        if (dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата начала события должна быть минимум через 2 часа от текущего момента");
        }

        User initiator = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        Event event = mapper.toEvent(dto,
                categoryService.getCategoryById(dto.getCategory()),
                locationService.getOrCreateLocation(dto.getLocation()),
                initiator);

        event.setCreatedOn(LocalDateTime.now());
        event.setState(EventState.PENDING);

        Event saved = eventRepository.save(event);

        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(saved.getId());
        return mapper.toEventFullDto(saved, confirmedRequests, 0L); // views = 0 для нового события
    }

    @Transactional
    public EventFullDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest dto) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не может изменять чужое событие");
        }
        if (!(event.getState() == EventState.PENDING || event.getState() == EventState.CANCELED)) {
            throw new ConflictException("Изменять можно только события в состоянии ожидания или отмененные");
        }
        if (dto.getEventDate() != null &&
                dto.getEventDate().isBefore(LocalDateTime.now().plusHours(2))) {
            throw new BadRequestException("Дата события должна быть минимум через 2 часа от текущего момента");
        }

        if (dto.getAnnotation() != null) event.setAnnotation(dto.getAnnotation());
        if (dto.getCategory() != null) event.setCategory(categoryService.getCategoryById(dto.getCategory()));
        if (dto.getDescription() != null) event.setDescription(dto.getDescription());
        if (dto.getEventDate() != null) event.setEventDate(dto.getEventDate());
        if (dto.getLocation() != null) event.setLocation(locationService.getOrCreateLocation(dto.getLocation()));
        if (dto.getPaid() != null) event.setPaid(dto.getPaid());
        if (dto.getParticipantLimit() != null) event.setParticipantLimit(dto.getParticipantLimit());
        if (dto.getRequestModeration() != null) event.setRequestModeration(dto.getRequestModeration());
        if (dto.getTitle() != null) event.setTitle(dto.getTitle());
        if (dto.getStateAction() != null) {
            switch (dto.getStateAction()) {
                case SEND_TO_REVIEW -> event.setState(EventState.PENDING);
                case CANCEL_REVIEW -> event.setState(EventState.CANCELED);
            }
        }

        Event updated = eventRepository.save(event);

        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(updated.getId());
        return mapper.toEventFullDto(updated, confirmedRequests, 0L); // views 0
    }

    public List<EventShortDto> getUserEvents(Long userId, int from, int size) {

        userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id=" + userId + " не найден"));

        List<Event> events = eventRepository.findAllByInitiatorId(userId, PageRequest.of(from / size, size))
                .getContent();

        return convertToEventShortDtoList(events);
    }

    public EventFullDto getUserEvent(Long userId, Long eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие с id=" + eventId + " не найдено"));

        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException("Пользователь не может просматривать чужое событие");
        }

        Long confirmedRequests = requestRepository.countConfirmedRequestsByEventId(eventId);
        return mapper.toEventFullDto(event, confirmedRequests, 0L); // views 0
    }

    // вспомогательный метод для конвертации списка
    private List<EventShortDto> convertToEventShortDtoList(List<Event> events) {
        if (events.isEmpty()) {
            return List.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).collect(Collectors.toList());
        Map<Long, Long> confirmedRequestsMap = getConfirmedRequestsMap(eventIds);

        return events.stream()
                .map(event -> mapper.toEventShortDto(event,
                        confirmedRequestsMap.getOrDefault(event.getId(), 0L),
                        0L)) // views пока 0
                .collect(Collectors.toList());
    }

    // метод для получения подтвержденных запросов
    private Map<Long, Long> getConfirmedRequestsMap(List<Long> eventIds) {
        List<Object[]> results = requestRepository.countConfirmedRequestsByEventIds(eventIds);
        return results.stream()
                .collect(Collectors.toMap(
                        result -> (Long) result[0],
                        result -> (Long) result[1]
                ));
    }
}