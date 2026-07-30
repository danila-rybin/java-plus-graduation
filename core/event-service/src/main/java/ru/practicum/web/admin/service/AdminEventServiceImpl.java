package ru.practicum.web.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.web.category.entity.Category;
import ru.practicum.web.category.repository.CategoryRepository;
import ru.practicum.dto.EventDto;
import ru.practicum.dto.UpdateEventAdminRequest;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.validation.ValidationConstants;
import ru.practicum.web.admin.mapper.AdminEventMapperService;
import ru.practicum.web.admin.utils.DateUtils;
import ru.practicum.web.admin.validation.AdminEventValidator;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.entity.EventStatus;
import ru.practicum.web.event.mapper.EventMapper;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.stats.StatsService;


import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminEventServiceImpl implements AdminEventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final StatsService statsService;
    private final AdminEventValidator validator;
    private final AdminEventMapperService mapperService;
    private final DateUtils dateUtils;

    @Override
    public void updateConfirmedRequests(Long eventId, int delta) {
        log.info("Обновление количества подтвержденных заявок для события {} на delta={}", eventId, delta);

        Event event = getEventOrThrow(eventId);
        long oldValue = event.getConfirmedRequests() != null ? event.getConfirmedRequests() : 0L;
        long updated = oldValue + delta;

        if (updated < 0) {
            log.warn("Попытка установить отрицательное количество подтвержденных заявок для события {}", eventId);
            throw new IllegalArgumentException("Confirmed requests cannot be negative");
        }

        event.setConfirmedRequests(updated);
        eventRepository.save(event);

        log.info("Количество подтвержденных заявок для события {} обновлено: {} -> {}", eventId, oldValue, updated);
    }

    @Override
    public List<EventDto> getEvents(List<Long> users,
                                    List<String> states,
                                    List<Long> categories,
                                    String rangeStart,
                                    String rangeEnd,
                                    int from,
                                    int size) {

        log.info("Запрос списка событий администратором с параметрами: users={}, states={}, categories={}, " +
                "rangeStart={}, rangeEnd={}, from={}, size={}", users, states, categories, rangeStart, rangeEnd, from, size);

        validatePagination(from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        LocalDateTime start = dateUtils.parseDateTime(rangeStart);
        LocalDateTime end = dateUtils.parseDateTime(rangeEnd);

        List<EventStatus> statusEnums = parseStates(states);

        Page<Event> eventPage = eventRepository.findEventsByAdminFilters(
                users != null && !users.isEmpty() ? users : null,
                statusEnums,
                categories != null && !categories.isEmpty() ? categories : null,
                start,
                end,
                pageable
        );

        List<Event> events = eventPage.getContent();
        log.debug("Найдено {} событий", events.size());

        statsService.setViewsForEvents(events);

        return events.stream()
                .map(EventMapper::toDto)
                .toList();
    }

    @Override
    public EventDto updateEvent(Long eventId, UpdateEventAdminRequest request) {
        log.info("Обновление события с id={} администратором", eventId);

        Event event = getEventOrThrow(eventId);
        log.info("Найдено событие с id={}, status={}", event.getId(), event.getStatus());

        if (request.getStateAction() != null) {
            log.info("Обработка stateAction={} для события с id={}", request.getStateAction(), eventId);
            handleStateAction(event, request.getStateAction());
            log.info("Статус события с id={} изменен на {}", eventId, event.getStatus());
        }

        if (request.getTitle() != null) {
            validator.validateTitle(request.getTitle());
        }
        if (request.getAnnotation() != null) {
            validator.validateAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            validator.validateDescription(request.getDescription());
        }
        if (request.getParticipantLimit() != null) {
            validator.validateParticipantLimit(request.getParticipantLimit());
        }

        LocalDateTime eventDate = null;
        if (request.getEventDate() != null) {
            eventDate = dateUtils.parseEventDate(request.getEventDate());
            validator.validateEventDate(eventDate, request.getEventDate());
        }

        Category category = getCategoryIfPresent(request.getCategory());

        mapperService.updateEventFields(event, request, eventDate, category);

        Event savedEvent = eventRepository.save(event);
        log.info("Событие с id={} обновлено, новый статус={}", savedEvent.getId(), savedEvent.getStatus());

        Long views = statsService.getViews(savedEvent);
        savedEvent.setViews(views);

        log.info("Событие с id={} успешно обновлено", eventId);
        return EventMapper.toDto(savedEvent);
    }

    private void validatePagination(int from, int size) {
        if (from < ValidationConstants.PAGE_MIN_FROM) {
            log.warn("Некорректное значение параметра from: {}", from);
            throw new BadRequestException("Parameter 'from' must be non-negative");
        }
        if (size < ValidationConstants.PAGE_MIN_SIZE) {
            log.warn("Некорректное значение параметра size: {}", size);
            throw new BadRequestException("Parameter 'size' must be positive");
        }
    }

    private List<EventStatus> parseStates(List<String> states) {
        if (states == null || states.isEmpty()) {
            return null;
        }
        try {
            return states.stream()
                    .map(EventStatus::valueOf)
                    .toList();
        } catch (IllegalArgumentException e) {
            log.warn("Некорректное значение статуса: {}", states);
            throw new BadRequestException("Invalid state value: " + states);
        }
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Событие с id={} не найдено", eventId);
                    return new NotFoundException("Event with id=" + eventId + " was not found");
                });
    }

    private Category getCategoryIfPresent(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> {
                    log.warn("Категория с id={} не найдена", categoryId);
                    return new NotFoundException("Category with id=" + categoryId + " not found");
                });
    }

    private void handleStateAction(Event event, String stateAction) {
        switch (stateAction) {
            case "PUBLISH_EVENT":
                if (event.getStatus() != EventStatus.PENDING) {
                    throw new ConflictException("Cannot publish the event because it's not in PENDING state. Current status: " + event.getStatus());
                }
                if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(ValidationConstants.EVENT_PUBLISH_HOURS_BEFORE))) {
                    throw new ConflictException("Cannot publish event because event date is too soon");
                }
                event.setStatus(EventStatus.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                log.debug("Событие {} опубликовано", event.getId());
                break;
            case "REJECT_EVENT":
                if (event.getStatus() == EventStatus.PUBLISHED) {
                    throw new ConflictException("Cannot reject already published event");
                }
                event.setStatus(EventStatus.CANCELED);
                log.debug("Событие {} отклонено", event.getId());
                break;
            case "CANCEL_EVENT":
                if (event.getStatus() == EventStatus.PUBLISHED) {
                    throw new ConflictException("Cannot cancel already published event");
                }
                event.setStatus(EventStatus.CANCELED);
                log.debug("Событие {} отменено", event.getId());
                break;
            default:
                log.warn("Некорректное действие со статусом: {}", stateAction);
                throw new BadRequestException("Invalid state action: " + stateAction);
        }
    }

    @Override
    public boolean existsByCategoryId(Long categoryId) {
        log.debug("Проверка использования категории с id={} в событиях", categoryId);
        boolean exists = eventRepository.existsByCategoryId(categoryId);
        log.debug("Категория с id={} используется в событиях: {}", categoryId, exists);
        return exists;
    }

    @Override
    public EventDto getEventById(Long eventId) {
        log.debug("Запрос события с id={}", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Event with id=" + eventId + " was not found"));

        Long views = statsService.getViews(event);
        event.setViews(views);

        return EventMapper.toDto(event);
    }
}
