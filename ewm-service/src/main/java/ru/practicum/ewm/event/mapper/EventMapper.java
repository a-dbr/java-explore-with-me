package ru.practicum.ewm.event.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.ewm.category.model.Category;
import ru.practicum.ewm.event.dto.EventFullDto;
import ru.practicum.ewm.event.dto.EventShortDto;
import ru.practicum.ewm.event.dto.NewEventDto;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.location.model.Location;
import ru.practicum.ewm.user.model.User;

@Mapper(componentModel = "spring")
public interface EventMapper {
    EventFullDto toEventFullDto(Event event);

    EventFullDto toEventFullDto(Event event, Long confirmedRequests, Long views, Long commentCount);

    EventShortDto toEventShortDto(Event event);

    EventShortDto toEventShortDto(Event event, Long confirmedRequests, Long views, Long commentCount);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", source = "categoryById")
    @Mapping(target = "location", source = "orCreateLocation")
    @Mapping(target = "initiator", source = "initiator")
    @Mapping(target = "paid", expression = "java(dto.getPaid() == null ? false : dto.getPaid())")
    @Mapping(target = "participantLimit",
            expression = "java(dto.getParticipantLimit() == null ? 0 : dto.getParticipantLimit())")
    @Mapping(target = "requestModeration",
            expression = "java(dto.getRequestModeration() == null ? true : dto.getRequestModeration())")
    Event toEvent(NewEventDto dto, Category categoryById, Location orCreateLocation, User initiator);
}