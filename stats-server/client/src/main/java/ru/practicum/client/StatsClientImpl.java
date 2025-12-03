package ru.practicum.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

@Component
public class StatsClientImpl implements StatsClient {
    private final RestClient restClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public StatsClientImpl(@Value("${stats-server.url:http://localhost:9090}") String clientUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(clientUrl)
                .build();
    }

    @Override
    public void hit(EndpointHitDto endpointHitDto) {
        restClient.post()
                .uri("/hit")
                .contentType(MediaType.APPLICATION_JSON)
                .body(endpointHitDto)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public Collection<ViewStatsDto> getStat(String start, String end, List<String> urls, Boolean unique) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("диапазон не может содержать null");
        }
        LocalDateTime startDataTime = LocalDateTime.parse(start, formatter);
        LocalDateTime endDataTime = LocalDateTime.parse(end, formatter);
        if (startDataTime.isAfter(endDataTime)) {
            throw new IllegalArgumentException("задан неверный диапазон");
        }
        return restClient.get()
                .uri(uriBuilder -> uriGetStats(uriBuilder, start, end, urls, unique))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {
                });
    }

    private URI uriGetStats(UriBuilder uriBuilder, String start, String end, List<String> uris, Boolean unique) {
        UriBuilder builder = uriBuilder.path("/stats")
                .queryParam("start", start)
                .queryParam("end", end);
        if (uris != null && !uris.isEmpty()) {
            uris.forEach(url -> builder.queryParam("uris", url));
        }
        if (unique != null) {
            builder.queryParam("unique", unique);
        }
        return builder.build();
    }
}