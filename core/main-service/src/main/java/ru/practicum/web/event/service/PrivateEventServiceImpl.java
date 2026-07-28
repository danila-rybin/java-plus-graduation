package ru.practicum.web.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.ViewStatsDto;
import ru.practicum.statsclient.StatsClient;
import ru.practicum.web.admin.repository.CategoryRepository;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.dto.EventShortDto;
import ru.practicum.web.event.dto.NewEventDto;
import ru.practicum.web.event.dto.UpdateEventUserRequest;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.entity.EventStatus;
import ru.practicum.web.event.mapper.EventMapper;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.exception.BadRequestException;
import ru.practicum.web.exception.ConflictException;
import ru.practicum.web.exception.NotFoundException;
import ru.practicum.web.user.repository.UserRepository;
import ru.practicum.web.validation.ValidationConstants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PrivateEventServiceImpl implements PrivateEventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final StatsClient statsClient;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(ValidationConstants.DATE_TIME_FORMAT);

    @Override
    public List<EventShortDto> getEvents(Long userId, int from, int size) {
        log.info("Запрос списка событий пользователя с id={}, from={}, size={}", userId, from, size);

        if (!userRepository.existsById(userId)) {
            log.warn("Пользователь с id={} не найден", userId);
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        List<Event> events = eventRepository.findByInitiatorId(userId, pageable).getContent();
        log.debug("Найдено {} событий", events.size());

        Map<Long, Long> viewsMap = getViewsMap(events);

        return events.stream()
                .map(event -> {
                    EventShortDto dto = EventMapper.toShortDto(event);
                    dto.setViews(viewsMap.getOrDefault(event.getId(), ValidationConstants.DEFAULT_VIEWS));
                    dto.setConfirmedRequests(event.getConfirmedRequests() != null ?
                            event.getConfirmedRequests() : ValidationConstants.DEFAULT_CONFIRMED_REQUESTS);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    public EventDto addEvent(Long userId, NewEventDto dto) {
        log.info("Создание нового события пользователем с id={}", userId);

        var user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Пользователь с id={} не найден", userId);
                    return new NotFoundException("User with id=" + userId + " was not found");
                });

        var category = categoryRepository.findById(dto.getCategory())
                .orElseThrow(() -> {
                    log.warn("Категория с id={} не найдена", dto.getCategory());
                    return new NotFoundException("Category with id=" + dto.getCategory() + " was not found");
                });

        LocalDateTime eventDate = parseDateTime(dto.getEventDate());
        if (eventDate.isBefore(LocalDateTime.now().plusHours(ValidationConstants.EVENT_HOURS_BEFORE_START))) {
            log.warn("Дата события {} должна быть не ранее чем через 2 часа", dto.getEventDate());
            throw new BadRequestException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: " + dto.getEventDate());
        }

        Event event = Event.builder()
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .description(dto.getDescription())
                .eventDate(eventDate)
                .initiator(user)
                .category(category)
                .location(dto.getLocation())
                .paid(dto.getPaid() != null ? dto.getPaid() : false)
                .participantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0)
                .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
                .status(EventStatus.PENDING)
                .createdOn(LocalDateTime.now())
                .confirmedRequests(ValidationConstants.DEFAULT_CONFIRMED_REQUESTS)
                .views(ValidationConstants.DEFAULT_VIEWS)
                .build();

        Event saved = eventRepository.save(event);
        log.info("Событие создано с id={}", saved.getId());
        return EventMapper.toDto(saved);
    }

    @Override
    public EventDto getEvent(Long userId, Long eventId) {
        log.info("Запрос события с id={} для пользователя с id={}", eventId, userId);

        if (!userRepository.existsById(userId)) {
            log.warn("Пользователь с id={} не найден", userId);
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> {
                    log.warn("Событие с id={} для пользователя {} не найдено", eventId, userId);
                    return new NotFoundException("Event with id=" + eventId + " was not found");
                });

        Long views = getViewsForEvent(event);
        event.setViews(views);

        return EventMapper.toDto(event);
    }

    @Override
    public EventDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Обновление события с id={} пользователем с id={}", eventId, userId);

        if (!userRepository.existsById(userId)) {
            log.warn("Пользователь с id={} не найден", userId);
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> {
                    log.warn("Событие с id={} для пользователя {} не найдено", eventId, userId);
                    return new NotFoundException("Event with id=" + eventId + " was not found");
                });

        if (event.getStatus() == EventStatus.PUBLISHED) {
            log.warn("Попытка изменить опубликованное событие с id={}", eventId);
            throw new ConflictException("Only pending or canceled events can be changed");
        }

        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
            log.debug("Обновлен заголовок события");
        }
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
            log.debug("Обновлена аннотация события");
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
            log.debug("Обновлено описание события");
        }
        if (updateRequest.getEventDate() != null) {
            LocalDateTime newEventDate = parseDateTime(updateRequest.getEventDate());
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(ValidationConstants.EVENT_HOURS_BEFORE_START))) {
                log.warn("Дата события {} должна быть не ранее чем через 2 часа", updateRequest.getEventDate());
                throw new BadRequestException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: " + updateRequest.getEventDate());
            }
            event.setEventDate(newEventDate);
            log.debug("Обновлена дата события");
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
            log.debug("Обновлен статус платности события: {}", updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            if (updateRequest.getParticipantLimit() < ValidationConstants.EVENT_PARTICIPANT_LIMIT_MIN) {
                log.warn("Некорректный лимит участников: {}", updateRequest.getParticipantLimit());
                throw new BadRequestException("Participant limit must be non-negative");
            }
            event.setParticipantLimit(updateRequest.getParticipantLimit());
            log.debug("Обновлен лимит участников: {}", updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
            log.debug("Обновлен статус модерации запросов: {}", updateRequest.getRequestModeration());
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(updateRequest.getLocation());
            log.debug("Обновлена локация события");
        }
        if (updateRequest.getCategory() != null) {
            var category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> {
                        log.warn("Категория с id={} не найдена", updateRequest.getCategory());
                        return new NotFoundException("Category with id=" + updateRequest.getCategory() + " was not found");
                    });
            event.setCategory(category);
            log.debug("Обновлена категория события");
        }

        if (updateRequest.getStateAction() != null) {
            log.debug("Обработка действия со статусом: {}", updateRequest.getStateAction());
            switch (updateRequest.getStateAction()) {
                case "SEND_TO_REVIEW":
                    event.setStatus(EventStatus.PENDING);
                    log.debug("Событие отправлено на модерацию");
                    break;
                case "CANCEL_REVIEW":
                    event.setStatus(EventStatus.CANCELED);
                    log.debug("Модерация события отменена");
                    break;
            }
        }

        Event updated = eventRepository.save(event);
        Long views = getViewsForEvent(updated);
        updated.setViews(views);

        log.info("Событие с id={} успешно обновлено", eventId);
        return EventMapper.toDto(updated);
    }

    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, FORMATTER);
        } catch (Exception e) {
            try {
                return LocalDateTime.parse(dateTimeStr);
            } catch (Exception e2) {
                log.warn("Ошибка парсинга даты: {}", dateTimeStr);
                throw new IllegalArgumentException("Invalid date format. Expected: " + ValidationConstants.DATE_TIME_FORMAT + " or ISO format");
            }
        }
    }

    private Map<Long, Long> getViewsMap(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<String> uris = events.stream()
                .map(e -> "/events/" + e.getId())
                .collect(Collectors.toList());

        LocalDateTime start = events.stream()
                .map(Event::getCreatedOn)
                .filter(Objects::nonNull)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusYears(1));

        try {
            List<ViewStatsDto> stats = statsClient.getStats(
                    start,
                    LocalDateTime.now(),
                    uris,
                    true
            );

            return stats.stream()
                    .filter(stat -> stat.getUri() != null)
                    .collect(Collectors.toMap(
                            stat -> extractEventIdFromUri(stat.getUri()),
                            ViewStatsDto::getHits,
                            (existing, replacement) -> existing
                    ));
        } catch (Exception e) {
            log.error("Ошибка получения статистики просмотров: {}", e.getMessage());
            return Map.of();
        }
    }

    private Long getViewsForEvent(Event event) {
        if (event.getId() == null) {
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

            return stats.isEmpty() ? ValidationConstants.DEFAULT_VIEWS : stats.getFirst().getHits();
        } catch (Exception e) {
            log.error("Ошибка получения просмотров для события {}: {}", event.getId(), e.getMessage());
            return ValidationConstants.DEFAULT_VIEWS;
        }
    }

    private Long extractEventIdFromUri(String uri) {
        try {
            String[] parts = uri.split("/");
            return Long.parseLong(parts[parts.length - 1]);
        } catch (Exception e) {
            log.error("Ошибка извлечения id из uri: {}", uri);
            return null;
        }
    }
}