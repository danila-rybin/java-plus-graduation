package ru.practicum.web.admin.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.entity.EventStatus;
import ru.practicum.web.exception.BadRequestException;
import ru.practicum.web.exception.ConflictException;
import ru.practicum.web.validation.ValidationConstants;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminEventValidator {

    public void validateTitle(String title) {
        if (title != null) {
            log.debug("Валидация заголовка: {}", title);
            if (title.length() < ValidationConstants.EVENT_TITLE_MIN ||
                    title.length() > ValidationConstants.EVENT_TITLE_MAX) {
                log.warn("Некорректная длина заголовка: {}, допустимо от {} до {}",
                        title.length(), ValidationConstants.EVENT_TITLE_MIN, ValidationConstants.EVENT_TITLE_MAX);
                throw new BadRequestException("Title length must be between " +
                        ValidationConstants.EVENT_TITLE_MIN + " and " + ValidationConstants.EVENT_TITLE_MAX + " characters");
            }
        }
    }

    public void validateAnnotation(String annotation) {
        if (annotation != null) {
            log.debug("Валидация аннотации, длина: {}", annotation.length());
            if (annotation.length() < ValidationConstants.EVENT_ANNOTATION_MIN ||
                    annotation.length() > ValidationConstants.EVENT_ANNOTATION_MAX) {
                log.warn("Некорректная длина аннотации: {}, допустимо от {} до {}",
                        annotation.length(), ValidationConstants.EVENT_ANNOTATION_MIN,
                        ValidationConstants.EVENT_ANNOTATION_MAX);
                throw new BadRequestException("Annotation length must be between " +
                        ValidationConstants.EVENT_ANNOTATION_MIN + " and " +
                        ValidationConstants.EVENT_ANNOTATION_MAX + " characters");
            }
        }
    }

    public void validateDescription(String description) {
        if (description != null) {
            log.debug("Валидация описания, длина: {}", description.length());
            if (description.length() < ValidationConstants.EVENT_DESCRIPTION_MIN ||
                    description.length() > ValidationConstants.EVENT_DESCRIPTION_MAX) {
                log.warn("Некорректная длина описания: {}, допустимо от {} до {}",
                        description.length(), ValidationConstants.EVENT_DESCRIPTION_MIN,
                        ValidationConstants.EVENT_DESCRIPTION_MAX);
                throw new BadRequestException("Description length must be between " +
                        ValidationConstants.EVENT_DESCRIPTION_MIN + " and " +
                        ValidationConstants.EVENT_DESCRIPTION_MAX + " characters");
            }
        }
    }

    public void validateEventDate(LocalDateTime eventDate, String originalDateStr) {
        if (eventDate == null) return;

        log.debug("Валидация даты события: {}", originalDateStr);
        if (eventDate.isBefore(LocalDateTime.now().plusHours(ValidationConstants.EVENT_HOURS_BEFORE_START))) {
            log.warn("Дата события {} должна быть не ранее чем через {} часа",
                    originalDateStr, ValidationConstants.EVENT_HOURS_BEFORE_START);
            throw new BadRequestException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: " + originalDateStr);
        }
    }

    public void validateParticipantLimit(Integer limit) {
        if (limit != null) {
            log.debug("Валидация лимита участников: {}", limit);
            if (limit < ValidationConstants.EVENT_PARTICIPANT_LIMIT_MIN) {
                log.warn("Некорректный лимит участников: {}", limit);
                throw new BadRequestException("Participant limit must be non-negative");
            }
        }
    }

    public void validatePublishEvent(Event event) {
        log.debug("Валидация публикации события: eventId={}, текущий статус={}",
                event.getId(), event.getStatus());

        if (event.getStatus() != EventStatus.PENDING) {
            log.warn("Невозможно опубликовать событие {} со статусом {}",
                    event.getId(), event.getStatus());
            throw new ConflictException("Cannot publish the event because it's not in the right state: " + event.getStatus());
        }
        if (event.getEventDate().isBefore(LocalDateTime.now().plusHours(ValidationConstants.EVENT_PUBLISH_HOURS_BEFORE))) {
            log.warn("Дата события {} слишком близка для публикации", event.getId());
            throw new ConflictException("Cannot publish event because event date is too soon");
        }

        log.debug("Валидация публикации события успешно пройдена");
    }

    public void validateRejectEvent(Event event) {
        log.debug("Валидация отклонения события: eventId={}, текущий статус={}",
                event.getId(), event.getStatus());

        if (event.getStatus() == EventStatus.PUBLISHED) {
            log.warn("Невозможно отклонить уже опубликованное событие {}", event.getId());
            throw new ConflictException("Cannot reject already published event");
        }

        log.debug("Валидация отклонения события успешно пройдена");
    }
}