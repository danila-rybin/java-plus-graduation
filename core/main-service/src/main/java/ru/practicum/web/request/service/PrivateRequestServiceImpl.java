package ru.practicum.web.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.exception.ConflictException;
import ru.practicum.web.exception.NotFoundException;
import ru.practicum.web.request.dto.EventRequestStatusUpdateRequest;
import ru.practicum.web.request.dto.EventRequestStatusUpdateResult;
import ru.practicum.web.request.dto.ParticipationRequestDto;
import ru.practicum.web.request.entity.ParticipationRequest;
import ru.practicum.web.request.entity.RequestStatus;
import ru.practicum.web.request.mapper.RequestMapperService;
import ru.practicum.web.request.repository.ParticipationRequestRepository;
import ru.practicum.web.request.validation.RequestValidator;
import ru.practicum.web.user.entity.User;
import ru.practicum.web.user.repository.UserRepository;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PrivateRequestServiceImpl implements PrivateRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final RequestValidator validator;
    private final RequestMapperService mapperService;
    private final RequestStatusUpdateService statusUpdateService;

    @Override
    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Запрос списка заявок пользователя с id={}", userId);
        checkUserExists(userId);

        List<ParticipationRequestDto> requests = requestRepository.findAllByRequesterId(userId).stream()
                .map(mapperService::toDto)
                .collect(Collectors.toList());

        log.debug("Найдено {} заявок", requests.size());
        return requests;
    }

    @Override
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        log.info("Создание заявки на участие в событии с id={} от пользователя с id={}", eventId, userId);

        User user = getUserOrThrow(userId);
        Event event = getEventOrThrow(eventId);

        validator.validateAddRequest(user, event, userId, eventId);

        ParticipationRequest request = mapperService.createRequest(user, event);
        ParticipationRequest saved = requestRepository.save(request);
        log.info("Заявка создана с id={}, статус: {}", saved.getId(), saved.getStatus());

        if (saved.getStatus() == RequestStatus.CONFIRMED) {
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
            log.debug("Количество подтвержденных заявок для события {} увеличено до {}",
                    eventId, event.getConfirmedRequests());
        }

        return mapperService.toDto(saved);
    }

    @Override
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Отмена заявки с id={} пользователем с id={}", requestId, userId);

        checkUserExists(userId);

        ParticipationRequest request = validator.validateAndGetRequestForCancellation(requestId, userId);
        request.setStatus(RequestStatus.CANCELED);

        ParticipationRequest saved = requestRepository.save(request);
        log.info("Заявка с id={} отменена", requestId);

        return mapperService.toDto(saved);
    }

    @Override
    public List<ParticipationRequestDto> getEventRequests(Long userId, Long eventId) {
        log.info("Запрос заявок на событие с id={} для пользователя с id={}", eventId, userId);

        checkUserExists(userId);
        Event event = getEventAndCheckOwnership(userId, eventId);

        List<ParticipationRequestDto> requests = requestRepository.findAllByEventId(eventId).stream()
                .map(mapperService::toDto)
                .collect(Collectors.toList());

        log.debug("Найдено {} заявок на событие {}", requests.size(), eventId);
        return requests;
    }

    @Override
    public EventRequestStatusUpdateResult updateRequestsStatus(
            Long userId,
            Long eventId,
            EventRequestStatusUpdateRequest statusUpdateRequest
    ) {
        log.info("Обновление статусов заявок на событие с id={} пользователем с id={}. Действие: {}",
                eventId, userId, statusUpdateRequest.getStatus());

        Event event = getEventAndCheckOwnership(userId, eventId);
        validator.validateEventForRequestUpdate(event);

        List<ParticipationRequest> requests = validator.validateAndGetRequestsForUpdate(
                statusUpdateRequest.getRequestIds(), eventId, statusUpdateRequest.getStatus());

        EventRequestStatusUpdateResult result;
        if ("CONFIRMED".equals(statusUpdateRequest.getStatus())) {
            int availableSlots = validator.checkAvailableSlots(event);
            log.debug("Доступно мест: {}", availableSlots);
            result = statusUpdateService.confirmRequests(requests, event, availableSlots);
        } else if ("REJECTED".equals(statusUpdateRequest.getStatus())) {
            result = statusUpdateService.rejectRequests(requests);
        } else {
            log.warn("Некорректный статус: {}", statusUpdateRequest.getStatus());
            throw new ConflictException("Invalid status: " + statusUpdateRequest.getStatus());
        }

        log.info("Статусы заявок обновлены: подтверждено {}, отклонено {}",
                result.getConfirmedRequests().size(), result.getRejectedRequests().size());
        return result;
    }

    private void checkUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            log.warn("Пользователь с id={} не найден", userId);
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Пользователь с id={} не найден", userId);
                    return new NotFoundException("User with id=" + userId + " was not found");
                });
    }

    private Event getEventOrThrow(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Событие с id={} не найдено", eventId);
                    return new NotFoundException("Event with id=" + eventId + " was not found");
                });
    }

    private Event getEventAndCheckOwnership(Long userId, Long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Событие с id={} не найдено", eventId);
                    return new NotFoundException("Event with id=" + eventId + " was not found");
                });

        validator.validateAndCheckEventOwnership(event, userId);
        return event;
    }
}