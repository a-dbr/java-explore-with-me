package ru.practicum.ewm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.client.StatsClient;
import ru.practicum.client.StatsClientImpl;

@Configuration
public class StatsConfig {

    @Value("${stats-server.url:http://localhost:9090}")
    private String statsServerUrl;

    @Bean
    public StatsClient statsClient(@Autowired(required = false) ObjectMapper objectMapper) {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
        return new StatsClientImpl(statsServerUrl, objectMapper);
    }
}