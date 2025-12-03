package ru.practicum.client;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.util.Collection;
import java.util.List;

public interface StatsClient {

    void hit(EndpointHitDto endpointHitDto);

    Collection<ViewStatsDto> getStat(String start, String end, List<String> urls, Boolean unique);
}
