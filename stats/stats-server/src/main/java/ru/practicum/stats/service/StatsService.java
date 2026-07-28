package ru.practicum.stats.service;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

public interface StatsService {

    void saveHit(EndpointHitDto dto);

    List<ViewStatsDto> getStats(
            LocalDateTime start,
            LocalDateTime end,
            boolean unique
    );

    List<ViewStatsDto> getStats(
            LocalDateTime start,
            LocalDateTime end,
            boolean unique,
            List<String> uris
    );
}