package ru.practicum.web.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EventDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.validation.ValidationConstants;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.entity.EventStatus;
import ru.practicum.web.event.mapper.EventMapper;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.event.repository.EventSpecification;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PublicEventServiceImpl implements PublicEventService {

    private final EventRepository eventRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(ValidationConstants.DATE_TIME_FORMAT);

    @Override
    public EventDto getEvent(Long id) {
        log.info("Запрос события с id={}", id);

        Event event = eventRepository.findByIdAndStatus(id, EventStatus.PUBLISHED)
                .orElseThrow(() -> new NotFoundException(
                        "Event with id=" + id + " was not found"));

        return EventMapper.toDto(event);
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
            int size) {

        log.info("Публичный запрос событий с параметрами: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, " +
                "onlyAvailable={}, sort={}, from={}, size={}", text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        if (from < ValidationConstants.PAGE_MIN_FROM) {
            throw new BadRequestException("Parameter 'from' must be non-negative");
        }

        int actualSize = size > 0 ? size : ValidationConstants.PAGE_DEFAULT_SIZE;
        int page = from / actualSize;
        Pageable pageable = PageRequest.of(page, actualSize, getSort(sort));

        LocalDateTime startDateTime = parseDateTimeSafe(rangeStart);
        LocalDateTime endDateTime = parseDateTimeSafe(rangeEnd);

        if (startDateTime != null && endDateTime != null && startDateTime.isAfter(endDateTime)) {
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

        return events.stream()
                .map(EventMapper::toShortDto)
                .toList();
    }

    private Sort getSort(String sort) {
        if (sort == null) {
            return Sort.unsorted();
        }
        if ("EVENT_DATE".equals(sort)) {
            return Sort.by(Sort.Direction.ASC, "eventDate");
        }
        return Sort.unsorted();
    }

    private LocalDateTime parseDateTimeSafe(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, FORMATTER);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date format. Expected: " + ValidationConstants.DATE_TIME_FORMAT);
        }
    }
}