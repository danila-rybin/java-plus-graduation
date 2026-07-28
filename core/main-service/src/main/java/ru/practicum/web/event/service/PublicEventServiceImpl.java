package ru.practicum.web.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.dto.EventShortDto;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.entity.EventStatus;
import ru.practicum.web.event.mapper.EventMapper;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.event.repository.EventSpecification;
import ru.practicum.web.exception.BadRequestException;
import ru.practicum.web.exception.NotFoundException;
import jakarta.transaction.Transactional;
import ru.practicum.web.validation.ValidationConstants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PublicEventServiceImpl implements PublicEventService {

    private final EventRepository eventRepository;
    private final StatsClient statsClient;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(ValidationConstants.DATE_TIME_FORMAT);

    @Override
    public EventDto getEvent(Long id) {
        log.info("Запрос события с id={}", id);

        Event event = eventRepository.findByIdAndStatus(id, EventStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(
                        "Event with id=" + id + " was not found"));

        Long views = getViewsForEvent(event);

        EventDto dto = EventMapper.toDto(event);
        dto.setViews(views);

        return dto;
    }

    @Override
    public List<EventShortDto> getEvents(
            String text,
            List<Long> categories,
            Boolean paid,
            String rangeStart,
            String rangeEnd,
            Boolean onlyAvailable,
            String sort,
            int from,
            int size
    ) {
        log.info("Публичный запрос событий с параметрами: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, " +
                "onlyAvailable={}, sort={}, from={}, size={}", text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        if (from < ValidationConstants.PAGE_MIN_FROM) {
            log.warn("Некорректное значение from: {}", from);
            throw new BadRequestException("Parameter 'from' must be non-negative");
        }

        int actualSize = size > 0 ? size : ValidationConstants.PAGE_DEFAULT_SIZE;
        int page = from / actualSize;
        Pageable pageable = PageRequest.of(page, actualSize, getSort(sort));

        LocalDateTime startDateTime = null;
        LocalDateTime endDateTime = null;

        if (rangeStart != null && !rangeStart.isBlank()) {
            try {
                startDateTime = parseDateTime(rangeStart);
            } catch (Exception e) {
                log.warn("Ошибка парсинга даты начала: {}", rangeStart);
                throw new BadRequestException("Invalid date format for rangeStart");
            }
        }

        if (rangeEnd != null && !rangeEnd.isBlank()) {
            try {
                endDateTime = parseDateTime(rangeEnd);
            } catch (Exception e) {
                log.warn("Ошибка парсинга даты окончания: {}", rangeEnd);
                throw new BadRequestException("Invalid date format for rangeEnd");
            }
        }

        if (startDateTime != null && endDateTime != null && startDateTime.isAfter(endDateTime)) {
            log.warn("Дата начала {} позже даты окончания {}", rangeStart, rangeEnd);
            throw new BadRequestException("rangeStart must be before rangeEnd");
        }

        Page<Event> eventPage = eventRepository.findAll(
                EventSpecification.publicEvents(
                        text,
                        categories,
                        paid,
                        startDateTime,
                        endDateTime,
                        onlyAvailable
                ),
                pageable
        );

        List<Event> events = eventPage.getContent();
        log.info("Найдено {} событий", events.size());

        if (events.isEmpty()) {
            return new ArrayList<>();
        }

        Map<Long, Long> viewsMap = getViewsMap(events);

        List<EventShortDto> result = events.stream()
                .map(event -> {
                    Long views = viewsMap.getOrDefault(event.getId(), ValidationConstants.DEFAULT_VIEWS);
                    EventShortDto dto = EventMapper.toShortDto(event);
                    dto.setViews(views);
                    dto.setConfirmedRequests(event.getConfirmedRequests() != null ?
                            event.getConfirmedRequests() : ValidationConstants.DEFAULT_CONFIRMED_REQUESTS);
                    return dto;
                })
                .collect(Collectors.toList());

        // Сортировка по просмотрам в Java (так как views - transient поле)
        if ("VIEWS".equals(sort)) {
            result.sort((e1, e2) -> {
                if (e1.getViews() == null || e2.getViews() == null) return 0;
                return e1.getViews().compareTo(e2.getViews());
            });
            log.debug("Выполнена сортировка по просмотрам");
        }

        return result;
    }

    private Sort getSort(String sort) {
        if (sort == null) {
            return Sort.unsorted();
        }
        if ("EVENT_DATE".equals(sort)) {
            log.debug("Сортировка по дате события");
            return Sort.by(Sort.Direction.ASC, "eventDate");
        }
        return Sort.unsorted();
    }

    private Map<Long, Long> getViewsMap(List<Event> events) {
        if (events == null || events.isEmpty()) {
            return Map.of();
        }

        List<String> uris = events.stream()
                .filter(e -> e.getId() != null)
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        if (uris.isEmpty()) {
            return Map.of();
        }

        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    LocalDateTime.now().minusYears(1),
                    LocalDateTime.now(),
                    uris,
                    true
            );

            if (stats == null || stats.isEmpty()) {
                log.debug("Статистика просмотров не найдена");
                return events.stream()
                        .filter(e -> e.getId() != null)
                        .collect(Collectors.toMap(
                                Event::getId,
                                e -> ValidationConstants.DEFAULT_VIEWS
                        ));
            }

            Map<Long, Long> viewsMap = stats.stream()
                    .filter(stat -> stat != null && stat.getUri() != null)
                    .map(stat -> {
                        Long id = extractEventIdFromUri(stat.getUri());
                        return id != null ? Map.entry(id, stat.getHits()) : null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            Map.Entry::getValue,
                            (a, b) -> a
                    ));

            // Добавляем события без статистики
            events.stream()
                    .filter(e -> e.getId() != null && !viewsMap.containsKey(e.getId()))
                    .forEach(e -> viewsMap.put(e.getId(), ValidationConstants.DEFAULT_VIEWS));

            log.debug("Получена статистика для {} событий", viewsMap.size());
            return viewsMap;

        } catch (Exception e) {
            log.error("Ошибка получения статистики просмотров: {}", e.getMessage());
            return events.stream()
                    .filter(ee -> ee.getId() != null)
                    .collect(Collectors.toMap(
                            Event::getId,
                            ee -> ValidationConstants.DEFAULT_VIEWS
                    ));
        }
    }

    private Long getViewsForEvent(Event event) {
        if (event == null || event.getId() == null) {
            return ValidationConstants.DEFAULT_VIEWS;
        }

        try {
            LocalDateTime start = event.getCreatedOn() != null ?
                    event.getCreatedOn() : LocalDateTime.now().minusYears(1);

            String uri = "/events/" + event.getId();

            List<ViewStatsDto> stats = statsClient.getStats(
                    start,
                    LocalDateTime.now(),
                    List.of(uri),
                    true
            );

            if (stats == null || stats.isEmpty()) {
                return ValidationConstants.DEFAULT_VIEWS;
            }
            return stats.get(0).getHits();
        } catch (Exception e) {
            log.error("Ошибка получения просмотров для события {}: {}", event.getId(), e.getMessage());
            return ValidationConstants.DEFAULT_VIEWS;
        }
    }

    private Long extractEventIdFromUri(String uri) {
        if (uri == null) {
            return null;
        }
        try {
            String[] parts = uri.split("/");
            if (parts.length > 0) {
                return Long.parseLong(parts[parts.length - 1]);
            }
            return null;
        } catch (Exception e) {
            log.error("Ошибка извлечения id из uri: {}", uri);
            return null;
        }
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        try {
            return LocalDateTime.parse(dateTimeStr, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date format. Expected: " + ValidationConstants.DATE_TIME_FORMAT);
        }
    }
}