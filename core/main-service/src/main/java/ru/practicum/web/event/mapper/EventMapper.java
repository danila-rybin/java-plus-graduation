package ru.practicum.web.event.mapper;

import ru.practicum.web.admin.dto.CategoryDto;
import ru.practicum.web.admin.dto.UserShortDto;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.dto.EventShortDto;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.entity.EventStatus;
import ru.practicum.web.validation.ValidationConstants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EventMapper {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(ValidationConstants.DATE_TIME_FORMAT);

    public static EventDto toDto(Event event) {
        if (event == null) {
            return null;
        }

        EventDto dto = new EventDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setAnnotation(event.getAnnotation());
        dto.setDescription(event.getDescription());
        dto.setEventDate(event.getEventDate() != null ? event.getEventDate().format(FORMATTER) : null);
        dto.setState(event.getStatus() != null ? event.getStatus().name() : EventStatus.PENDING.name());
        dto.setPaid(event.getPaid() != null ? event.getPaid() : false);
        dto.setParticipantLimit(event.getParticipantLimit() != null ? event.getParticipantLimit() : 0);
        dto.setRequestModeration(event.getRequestModeration() != null ? event.getRequestModeration() : true);
        dto.setCreatedOn(event.getCreatedOn() != null ? event.getCreatedOn().format(FORMATTER) : null);
        dto.setPublishedOn(event.getPublishedOn() != null ? event.getPublishedOn().format(FORMATTER) : null);
        dto.setViews(event.getViews() != null ? event.getViews() : ValidationConstants.DEFAULT_VIEWS);
        dto.setConfirmedRequests(event.getConfirmedRequests() != null ? event.getConfirmedRequests() : ValidationConstants.DEFAULT_CONFIRMED_REQUESTS);
        dto.setLocation(event.getLocation());

        if (event.getCategory() != null) {
            CategoryDto categoryDto = new CategoryDto();
            categoryDto.setId(event.getCategory().getId());
            categoryDto.setName(event.getCategory().getName());
            dto.setCategory(categoryDto);
        }

        if (event.getInitiator() != null) {
            UserShortDto userDto = new UserShortDto();
            userDto.setId(event.getInitiator().getId());
            userDto.setName(event.getInitiator().getName());
            dto.setInitiator(userDto);
        }

        return dto;
    }

    public static Event toEntity(EventDto dto) {
        if (dto == null) {
            return null;
        }

        Event event = new Event();
        event.setId(dto.getId());
        event.setTitle(dto.getTitle());
        event.setAnnotation(dto.getAnnotation());
        event.setDescription(dto.getDescription());

        if (dto.getEventDate() != null && !dto.getEventDate().isEmpty()) {
            try {
                event.setEventDate(LocalDateTime.parse(dto.getEventDate(), FORMATTER));
            } catch (Exception e) {
                try {
                    event.setEventDate(LocalDateTime.parse(dto.getEventDate()));
                } catch (Exception e2) {
                    throw new IllegalArgumentException("Invalid date format: " + dto.getEventDate());
                }
            }
        }

        if (dto.getState() != null) {
            try {
                event.setStatus(EventStatus.valueOf(dto.getState()));
            } catch (IllegalArgumentException e) {
                event.setStatus(EventStatus.PENDING);
            }
        }

        event.setPaid(dto.getPaid() != null ? dto.getPaid() : false);
        event.setParticipantLimit(dto.getParticipantLimit() != null ? dto.getParticipantLimit() : 0);
        event.setRequestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true);
        event.setConfirmedRequests(dto.getConfirmedRequests() != null ? dto.getConfirmedRequests() : ValidationConstants.DEFAULT_CONFIRMED_REQUESTS);
        event.setViews(dto.getViews() != null ? dto.getViews() : ValidationConstants.DEFAULT_VIEWS);

        if (dto.getCreatedOn() != null && !dto.getCreatedOn().isEmpty()) {
            try {
                event.setCreatedOn(LocalDateTime.parse(dto.getCreatedOn(), FORMATTER));
            } catch (Exception e) {
                // Игнорируем ошибку парсинга
            }
        }

        return event;
    }

    public static void updateEntityFromDto(Event event, EventDto dto) {
        if (event == null || dto == null) {
            return;
        }

        if (dto.getTitle() != null) {
            event.setTitle(dto.getTitle());
        }

        if (dto.getAnnotation() != null) {
            event.setAnnotation(dto.getAnnotation());
        }

        if (dto.getDescription() != null) {
            event.setDescription(dto.getDescription());
        }

        if (dto.getEventDate() != null && !dto.getEventDate().isEmpty()) {
            try {
                event.setEventDate(LocalDateTime.parse(dto.getEventDate(), FORMATTER));
            } catch (Exception e) {
                // Игнорируем ошибку парсинга
            }
        }

        if (dto.getState() != null) {
            try {
                event.setStatus(EventStatus.valueOf(dto.getState()));
            } catch (IllegalArgumentException e) {
                // Игнорируем некорректный статус
            }
        }

        if (dto.getPaid() != null) {
            event.setPaid(dto.getPaid());
        }

        if (dto.getParticipantLimit() != null) {
            event.setParticipantLimit(dto.getParticipantLimit());
        }

        if (dto.getRequestModeration() != null) {
            event.setRequestModeration(dto.getRequestModeration());
        }

        if (dto.getConfirmedRequests() != null) {
            event.setConfirmedRequests(dto.getConfirmedRequests());
        }

        if (dto.getViews() != null) {
            event.setViews(dto.getViews());
        }
    }

    public static EventShortDto toShortDto(Event event) {
        if (event == null) {
            return null;
        }

        EventShortDto dto = new EventShortDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setAnnotation(event.getAnnotation());
        dto.setEventDate(event.getEventDate() != null ? event.getEventDate().format(FORMATTER) : null);
        dto.setPaid(event.getPaid() != null ? event.getPaid() : false);
        dto.setViews(event.getViews() != null ? event.getViews() : ValidationConstants.DEFAULT_VIEWS);
        dto.setConfirmedRequests(event.getConfirmedRequests() != null ? event.getConfirmedRequests() : ValidationConstants.DEFAULT_CONFIRMED_REQUESTS);
        dto.setLocation(event.getLocation());

        CategoryDto categoryDto = new CategoryDto();
        if (event.getCategory() != null) {
            categoryDto.setId(event.getCategory().getId());
            categoryDto.setName(event.getCategory().getName());
        } else {
            categoryDto.setId(ValidationConstants.DEFAULT_ID);
            categoryDto.setName(ValidationConstants.DEFAULT_NAME);
        }
        dto.setCategory(categoryDto);

        UserShortDto userDto = new UserShortDto();
        if (event.getInitiator() != null) {
            userDto.setId(event.getInitiator().getId());
            userDto.setName(event.getInitiator().getName());
        } else {
            userDto.setId(ValidationConstants.DEFAULT_ID);
            userDto.setName(ValidationConstants.DEFAULT_NAME);
        }
        dto.setInitiator(userDto);

        return dto;
    }
}