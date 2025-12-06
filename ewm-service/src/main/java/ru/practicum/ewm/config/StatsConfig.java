package ru.practicum.ewm.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.practicum.client.StatsClient;
import ru.practicum.client.StatsClientImpl;

@Configuration
public class StatsConfig {

    @Value("${stats-server.url}")
    private String statsServerUrl;

    @Bean
    public StatsClient statsClient() {
        return new StatsClientImpl(statsServerUrl);
    }
}