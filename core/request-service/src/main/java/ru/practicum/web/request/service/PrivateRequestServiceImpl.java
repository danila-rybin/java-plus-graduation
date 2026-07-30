package ru.practicum.web.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.EventClient;
import ru.practicum.feign.UserServiceClient;
import ru.practicum.dto.UserDto;
import ru.practicum.web.event.entity.Event;
import ru.practicum.dto.EventDto;
import ru.practicum.feign.AdminEventsClient;
import ru.practicum.web.event.entity.EventStatus;
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
    private final EventClient eventClient;
    private final AdminEventsClient adminEventsClient;
    private final RequestValidator validator;
    private final RequestMapperService mapperService;
    private final RequestStatusUpdateService statusUpdateService;
    private final UserServiceClient userServiceClient;

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
    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        log.info("Создание заявки на участие в событии с id={} от пользователя с id={}", eventId, userId);

        if (eventId == null || eventId <= 0) {
            log.warn("Некорректный eventId: {}", eventId);
            throw new BadRequestException("Event id is required and must be positive");
        }

        if (!userServiceClient.userExists(userId)) {
            log.warn("Пользователь с id={} не найден", userId);
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        Event event = getEventOrThrow(eventId);

        User user = getUserOrThrow(userId);

        validator.validateAddRequest(user, event, userId, eventId);

        ParticipationRequest request = mapperService.createRequest(user, event);
        ParticipationRequest saved = requestRepository.save(request);
        log.info("Заявка создана с id={}, статус: {}", saved.getId(), saved.getStatus());

        if (saved.getStatus() == RequestStatus.CONFIRMED) {
            try {
                adminEventsClient.updateConfirmedRequests(eventId, 1);
                log.debug("Запрошено увеличение числа подтвержденных заявок для события {} на 1", eventId);
            } catch (Exception e) {
                log.error("Ошибка при обновлении счетчика confirmedRequests: {}", e.getMessage());
            }
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
            log.info("Пользователь с id={} не найден в локальной БД, запрашиваем из user-service", userId);
            getUserOrCreate(userId);
        }
    }

    private User getUserOrThrow(Long userId) {
        return userRepository.findById(userId).orElseGet(() -> {
            log.info("Пользователь с id={} не найден в локальной БД, запрашиваем из user-service", userId);
            return getUserOrCreate(userId);
        });
    }

    private User getUserOrCreate(Long userId) {
        List<UserDto> users = userServiceClient.getUsers(List.of(userId));
        if (users.isEmpty()) {
            log.warn("Пользователь с id={} не найден в user-service", userId);
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
        UserDto userDto = users.get(0);
        User user = User.builder()
                .id(userDto.getId())
                .name(userDto.getName())
                .email(userDto.getEmail())
                .build();
        return userRepository.save(user);
    }

    private Event getEventOrThrow(Long eventId) {
        try {
            ResponseEntity<EventDto> response = eventClient.findById(eventId);
            EventDto dto = response.getBody();

            if (dto == null) {
                log.warn("Событие с id={} не найдено в event-service", eventId);
                throw new NotFoundException("Event with id=" + eventId + " was not found");
            }

            Integer participantLimit = dto.getParticipantLimit();
            if (participantLimit == null) {
                participantLimit = 0;
            }
            log.info("Событие {}: participantLimit={}, requestModeration={}",
                    eventId, participantLimit, dto.getRequestModeration());

            Event event = Event.builder()
                    .id(dto.getId())
                    .confirmedRequests(dto.getConfirmedRequests() != null ? dto.getConfirmedRequests() : 0L)
                    .participantLimit(participantLimit)
                    .requestModeration(dto.getRequestModeration() != null ? dto.getRequestModeration() : true)
                    .status(dto.getState() != null ? EventStatus.valueOf(dto.getState()) : null)
                    .build();

            if (dto.getInitiator() != null) {
                event.setInitiator(User.builder()
                        .id(dto.getInitiator().getId())
                        .name(dto.getInitiator().getName())
                        .build());
            }

            return event;
        } catch (Exception e) {
            log.error("Ошибка при запросе события {} у event-service: {}", eventId, e.getMessage());
            throw new NotFoundException("Event with id=" + eventId + " was not found");
        }
    }

    private Event getEventAndCheckOwnership(Long userId, Long eventId) {
        Event event = getEventOrThrow(eventId);
        validator.validateAndCheckEventOwnership(event, userId);
        return event;
    }
}