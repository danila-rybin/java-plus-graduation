package ru.practicum.web.request.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.entity.EventStatus;
import ru.practicum.web.exception.ConflictException;
import ru.practicum.web.exception.NotFoundException;
import ru.practicum.web.request.entity.ParticipationRequest;
import ru.practicum.web.request.entity.RequestStatus;
import ru.practicum.web.request.repository.ParticipationRequestRepository;
import ru.practicum.web.user.entity.User;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RequestValidator {

    private final ParticipationRequestRepository requestRepository;

    public void validateAddRequest(User user, Event event, Long userId, Long eventId) {
        log.debug("Валидация запроса на участие: userId={}, eventId={}", userId, eventId);

        if (event.getInitiator().getId().equals(userId)) {
            log.warn("Пользователь {} попытался подать заявку на свое событие {}", userId, eventId);
            throw new ConflictException("Initiator cannot participate in his own event");
        }
        if (event.getStatus() != EventStatus.PUBLISHED) {
            log.warn("Событие {} не опубликовано, статус: {}", eventId, event.getStatus());
            throw new ConflictException("Event is not published");
        }
        if (requestRepository.existsByEventIdAndRequesterId(eventId, userId)) {
            log.warn("Заявка от пользователя {} на событие {} уже существует", userId, eventId);
            throw new ConflictException("Request already exists");
        }

        int confirmedRequests = requestRepository.countByEventIdAndStatus(
                eventId, RequestStatus.CONFIRMED);
        if (event.getParticipantLimit() > 0 && confirmedRequests >= event.getParticipantLimit()) {
            log.warn("Достигнут лимит участников для события {}. Текущее количество: {}",
                    eventId, confirmedRequests);
            throw new ConflictException("Participant limit reached");
        }

        log.debug("Валидация запроса на участие успешно пройдена");
    }

    public ParticipationRequest validateAndGetRequestForCancellation(Long requestId, Long userId) {
        log.debug("Валидация отмены заявки: requestId={}, userId={}", requestId, userId);

        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> {
                    log.warn("Заявка с id={} не найдена", requestId);
                    return new NotFoundException("Request with id=" + requestId + " was not found");
                });

        if (!request.getRequester().getId().equals(userId)) {
            log.warn("Пользователь {} попытался отменить чужую заявку {}", userId, requestId);
            throw new NotFoundException("Request not found");
        }

        log.debug("Валидация отмены заявки успешно пройдена");
        return request;
    }

    public void validateAndCheckEventOwnership(Event event, Long userId) {
        log.debug("Проверка владения событием: eventId={}, userId={}", event.getId(), userId);

        if (!event.getInitiator().getId().equals(userId)) {
            log.warn("Пользователь {} не является владельцем события {}", userId, event.getId());
            throw new NotFoundException("Event with id=" + event.getId() + " was not found for user " + userId);
        }

        log.debug("Проверка владения событием успешно пройдена");
    }

    public void validateEventForRequestUpdate(Event event) {
        log.debug("Валидация события для обновления заявок: eventId={}", event.getId());

        if (event.getParticipantLimit() == 0) {
            log.warn("Для события {} лимит участников не ограничен", event.getId());
            throw new ConflictException("The participant limit has been reached");
        }
        if (!event.getRequestModeration()) {
            log.warn("Для события {} не требуется модерация заявок", event.getId());
            throw new ConflictException("The participant limit has been reached");
        }

        log.debug("Валидация события для обновления заявок успешно пройдена");
    }

    public List<ParticipationRequest> validateAndGetRequestsForUpdate(
            List<Long> requestIds, Long eventId, String status) {

        log.debug("Валидация запросов на обновление статуса: requestIds={}, eventId={}, status={}",
                requestIds, eventId, status);

        List<ParticipationRequest> requests = requestRepository.findAllById(requestIds);
        if (requests.isEmpty()) {
            log.warn("Список ID заявок пуст");
            throw new ConflictException("Request ids list is empty");
        }

        for (ParticipationRequest request : requests) {
            if (!request.getEvent().getId().equals(eventId)) {
                log.warn("Заявка {} не принадлежит событию {}", request.getId(), eventId);
                throw new NotFoundException("Request with id=" + request.getId() + " not found for this event");
            }
            if (request.getStatus() != RequestStatus.PENDING) {
                log.warn("Заявка {} имеет статус {}, ожидался PENDING",
                        request.getId(), request.getStatus());
                throw new ConflictException("Request must have status PENDING");
            }
        }

        log.debug("Валидация запросов на обновление статуса успешно пройдена, найдено {} заявок",
                requests.size());
        return requests;
    }

    public int checkAvailableSlots(Event event) {
        log.debug("Проверка доступных мест для события: eventId={}", event.getId());

        int confirmedRequests = requestRepository.countByEventIdAndStatus(
                event.getId(), RequestStatus.CONFIRMED);
        int participantLimit = event.getParticipantLimit();

        if (confirmedRequests >= participantLimit) {
            log.warn("Достигнут лимит участников для события {}. Текущее количество: {}",
                    event.getId(), confirmedRequests);
            throw new ConflictException("The participant limit has been reached");
        }

        int availableSlots = participantLimit - confirmedRequests;
        log.debug("Доступно мест для события {}: {}", event.getId(), availableSlots);
        return availableSlots;
    }
}