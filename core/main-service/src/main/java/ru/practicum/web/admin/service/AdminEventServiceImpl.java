package ru.practicum.web.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.practicum.web.admin.entity.UpdateEventAdminRequest;
import ru.practicum.web.admin.mapper.AdminEventMapperService;
import ru.practicum.web.admin.repository.CategoryRepository;
import ru.practicum.web.event.entity.EventStatus;
import ru.practicum.web.stats.StatsService;
import ru.practicum.web.admin.utils.DateUtils;
import ru.practicum.web.admin.validation.AdminEventValidator;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.mapper.EventMapper;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.exception.BadRequestException;
import ru.practicum.web.exception.NotFoundException;
import ru.practicum.web.validation.ValidationConstants;

import jakarta.transaction.Transactional;

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
        log.info("Обновление события с id={} администратором. Данные: {}", eventId, request);

        Event event = getEventOrThrow(eventId);

        // Валидация полей
        validator.validateTitle(request.getTitle());
        validator.validateAnnotation(request.getAnnotation());
        validator.validateDescription(request.getDescription());
        validator.validateParticipantLimit(request.getParticipantLimit());

        // Парсинг даты
        LocalDateTime eventDate = dateUtils.parseEventDate(request.getEventDate());
        validator.validateEventDate(eventDate, request.getEventDate());

        // Получение категории
        ru.practicum.web.admin.entity.Category category = getCategoryIfPresent(request.getCategory());

        // Обновление полей
        mapperService.updateEventFields(event, request, eventDate, category);

        // Обработка состояния
        if (request.getStateAction() != null) {
            log.debug("Обработка действия со статусом: {}", request.getStateAction());
            handleStateAction(event, request.getStateAction());
        }

        Event savedEvent = eventRepository.save(event);
        log.debug("Событие сохранено с id={}", savedEvent.getId());

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

    private ru.practicum.web.admin.entity.Category getCategoryIfPresent(Long categoryId) {
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
                validator.validatePublishEvent(event);
                mapperService.applyStateAction(event, stateAction);
                log.debug("Событие {} опубликовано", event.getId());
                break;
            case "REJECT_EVENT":
                validator.validateRejectEvent(event);
                mapperService.applyStateAction(event, stateAction);
                log.debug("Событие {} отклонено", event.getId());
                break;
            default:
                log.warn("Некорректное действие со статусом: {}", stateAction);
                throw new BadRequestException("Invalid state action: " + stateAction);
        }
    }
}