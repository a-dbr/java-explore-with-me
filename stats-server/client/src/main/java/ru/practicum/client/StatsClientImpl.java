package ru.practicum.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import ru.practicum.dto.ApiErrorDto;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.client.exception.StatsClientBadRequestException;
import ru.practicum.client.exception.StatsClientException;
import ru.practicum.client.exception.StatsClientUnavailableException;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;

@Component
public class StatsClientImpl implements StatsClient {
    private final RestClient restClient;
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ObjectMapper objectMapper;

    public StatsClientImpl(@Value("${stats-server.url:http://localhost:9090}") String clientUrl,
                           ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(clientUrl).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public void hit(EndpointHitDto endpointHitDto) {
        try {
            restClient.post()
                    .uri("/hit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(endpointHitDto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            handleHttpError(ex);
        } catch (RestClientException ex) {
            throw new StatsClientException("Ошибка связи со stats-server: " + ex.getMessage(), ex);
        }
    }

    @Override
    public Collection<ViewStatsDto> getStat(String start, String end, List<String> uris, Boolean unique) {
        if (start == null || end == null) {
            throw new IllegalArgumentException("диапазон не может содержать null");
        }
        LocalDateTime startDataTime = LocalDateTime.parse(start, formatter);
        LocalDateTime endDataTime = LocalDateTime.parse(end, formatter);
        if (startDataTime.isAfter(endDataTime)) {
            throw new IllegalArgumentException("задан неверный диапазон");
        }

        try {
            return restClient.get()
                    .uri(uriBuilder -> uriGetStats(uriBuilder, start, end, uris, unique))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientResponseException ex) {
            handleHttpError(ex);
            return List.of();
        } catch (RestClientException ex) {
            throw new StatsClientException("Ошибка при вызове stats-server: " + ex.getMessage(), ex);
        }
    }

    private void handleHttpError(RestClientResponseException ex) {
        String body = ex.getResponseBodyAsString();
        String msg = ex.getMessage();

        try {
            ApiErrorDto apiError = objectMapper.readValue(body, ApiErrorDto.class);
            if (apiError != null) {
                msg = apiError.getMessage() != null ? apiError.getMessage() : apiError.getReason();
            }
        } catch (Exception ignore) {
            // не удалось распарсить тело — используем исходное сообщение
        }

        HttpStatus status = (HttpStatus) ex.getStatusCode();
        if (status.is4xxClientError()) {
            throw new StatsClientBadRequestException(msg);
        } else if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            throw new StatsClientUnavailableException(msg);
        } else if (status.is5xxServerError()) {
            throw new StatsClientException(msg);
        }

        throw new StatsClientException(msg);
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
