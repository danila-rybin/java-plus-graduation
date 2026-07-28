package ru.practicum.web.stats;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.validation.ValidationConstants;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StatsService {

    private final StatsClient statsClient;

    public Long getViews(Event event) {
        if (statsClient == null || event.getId() == null) {
            return ValidationConstants.DEFAULT_VIEWS;
        }

        try {
            LocalDateTime start = event.getCreatedOn() != null ?
                    event.getCreatedOn() : LocalDateTime.now().minusYears(1);
            String uri = "/events/" + event.getId();

            List<ViewStatsDto> stats = statsClient.getStats(start, LocalDateTime.now(), List.of(uri), true);
            return stats.isEmpty() ? ValidationConstants.DEFAULT_VIEWS : stats.getFirst().getHits();
        } catch (Exception e) {
            log.error("Error getting views for event {}: {}", event.getId(), e.getMessage());
            return ValidationConstants.DEFAULT_VIEWS;
        }
    }

    public void setViewsForEvents(List<Event> events) {
        events.forEach(event -> {
            Long views = getViews(event);
            event.setViews(views);
        });
    }
}