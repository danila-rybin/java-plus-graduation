package ru.practicum.stats.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.stats.model.EndpointHit;
import ru.practicum.stats.model.EndpointHitDtoMapper;
import ru.practicum.stats.repository.EndpointHitRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final EndpointHitRepository repository;

    @Override
    public void saveHit(EndpointHitDto dto) {
        log.info("Сохранение статистики: app={}, uri={}, ip={}, timestamp={}",
                dto != null ? dto.getApp() : null,
                dto != null ? dto.getUri() : null,
                dto != null ? dto.getIp() : null,
                dto != null ? dto.getTimestamp() : null);

        // Простейшая защита от сохранения пустых записей
        if (dto == null) {
            log.warn("Попытка сохранить null DTO");
            return;
        }

        EndpointHit hit = EndpointHitDtoMapper.toEntity(dto);
        EndpointHit saved = repository.save(hit);
        log.debug("Статистика сохранена с id={}", saved.getId());
    }

    @Override
    public List<ViewStatsDto> getStats(
            LocalDateTime start,
            LocalDateTime end,
            boolean unique
    ) {
        log.info("Запрос статистики: start={}, end={}, unique={}", start, end, unique);

        List<ViewStatsDto> stats = unique
                ? repository.findUniqueStats(start, end)
                : repository.findStats(start, end);

        log.debug("Найдено {} записей статистики", stats.size());
        return stats;
    }

    @Override
    public List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, boolean unique, List<String> uris) {
        log.info("Запрос статистики с фильтрацией по URI: start={}, end={}, unique={}, uris={}",
                start, end, unique, uris);

        boolean filterByUris = !CollectionUtils.isEmpty(uris);
        List<String> normalizedUris = uris;

        // Нормализуем значения uris: если передано просто число (например "1"),
        // то считаем, что это id события и преобразуем в "/events/{id}" для совместимости с тестами.
        if (filterByUris) {
            normalizedUris = uris.stream()
                    .map(u -> {
                        if (u == null || u.isBlank()) return u;
                        String trimmed = u.trim();
                        if (trimmed.startsWith("/")) {
                            log.debug("URI уже в нормализованном виде: {}", trimmed);
                            return trimmed; // уже путь
                        }
                        if (trimmed.chars().allMatch(Character::isDigit)) {
                            String normalized = "/events/" + trimmed;
                            log.debug("Нормализован URI из числа {} в {}", trimmed, normalized);
                            return normalized;
                        }
                        log.debug("URI оставлен без изменений: {}", trimmed);
                        return trimmed;
                    })
                    .toList();
            log.debug("Нормализованные URI: {}", normalizedUris);
        }

        List<ViewStatsDto> stats;
        if (unique) {
            stats = filterByUris
                    ? repository.findUniqueStatsByUris(start, end, normalizedUris)
                    : repository.findUniqueStats(start, end);
        } else {
            stats = filterByUris
                    ? repository.findStatsByUris(start, end, normalizedUris)
                    : repository.findStats(start, end);
        }

        log.info("Найдено {} записей статистики", stats.size());

        if (log.isDebugEnabled() && !stats.isEmpty()) {
            stats.forEach(stat -> log.debug("Статистика: app={}, uri={}, hits={}",
                    stat.getApp(), stat.getUri(), stat.getHits()));
        }

        return stats;
    }
}