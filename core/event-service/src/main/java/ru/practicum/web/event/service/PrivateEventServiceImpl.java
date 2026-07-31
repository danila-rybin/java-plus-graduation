package ru.practicum.web.event.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.EventDto;
import ru.practicum.dto.EventShortDto;
import ru.practicum.dto.NewEventDto;
import ru.practicum.dto.UpdateEventUserRequest;
import ru.practicum.dto.UserDto;
import ru.practicum.exception.BadRequestException;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.UserServiceClient;
import ru.practicum.web.category.repository.CategoryRepository;
import ru.practicum.validation.ValidationConstants;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.entity.EventStatus;
import ru.practicum.web.event.mapper.EventMapper;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.user.entity.User;
import ru.practicum.web.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PrivateEventServiceImpl implements PrivateEventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final UserServiceClient userServiceClient;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(ValidationConstants.DATE_TIME_FORMAT);

    @Override
    public List<EventShortDto> getEvents(Long userId, int from, int size) {
        log.info("Запрос списка событий пользователя с id={}, from={}, size={}", userId, from, size);

        if (!userServiceClient.userExists(userId)) {
            log.warn("Пользователь с id={} не найден в user-service", userId);
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        List<Event> events = eventRepository.findByInitiatorId(userId, pageable).getContent();
        log.debug("Найдено {} событий", events.size());

        return events.stream()
                .map(EventMapper::toShortDto)
                .toList();
    }

    @Override
    public EventDto addEvent(Long userId, NewEventDto dto) {
        log.info("Создание события: userId={}, title={}", userId, dto.getTitle());

        if (!userServiceClient.userExists(userId)) {
            log.warn("Пользователь не найден в user-service: id={}", userId);
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        User user = getUserOrCreate(userId);

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
                .build();

        Event saved = eventRepository.save(event);
        log.info("Создание события: id={}, status={}, title={}, initiatorId={}",
                saved.getId(), saved.getStatus(), saved.getTitle(),
                saved.getInitiator() != null ? saved.getInitiator().getId() : "null");

        return EventMapper.toDto(saved);
    }

    @Override
    public EventDto getEvent(Long userId, Long eventId) {
        log.info("Запрос события с id={} для пользователя с id={}", eventId, userId);

        if (!userServiceClient.userExists(userId)) {
            log.warn("Пользователь с id={} не найден в user-service", userId);
            throw new NotFoundException("User with id=" + userId + " was not found");
        }

        Event event = eventRepository.findByIdAndInitiatorId(eventId, userId)
                .orElseThrow(() -> {
                    log.warn("Событие не найдено: id={}, userId={}", eventId, userId);
                    return new NotFoundException("Event with id=" + eventId + " was not found");
                });

        return EventMapper.toDto(event);
    }

    @Override
    public EventDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest) {
        log.info("Обновление события с id={} пользователем с id={}", eventId, userId);

        if (!userServiceClient.userExists(userId)) {
            log.warn("Пользователь с id={} не найден в user-service", userId);
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

        if (updateRequest.getTitle() != null) {
            event.setTitle(updateRequest.getTitle());
        }
        if (updateRequest.getAnnotation() != null) {
            event.setAnnotation(updateRequest.getAnnotation());
        }
        if (updateRequest.getDescription() != null) {
            event.setDescription(updateRequest.getDescription());
        }
        if (updateRequest.getEventDate() != null) {
            LocalDateTime newEventDate = parseDateTime(updateRequest.getEventDate());
            if (newEventDate.isBefore(LocalDateTime.now().plusHours(ValidationConstants.EVENT_HOURS_BEFORE_START))) {
                log.warn("Дата события {} должна быть не ранее чем через 2 часа", updateRequest.getEventDate());
                throw new BadRequestException("Field: eventDate. Error: должно содержать дату, которая еще не наступила. Value: " + updateRequest.getEventDate());
            }
            event.setEventDate(newEventDate);
        }
        if (updateRequest.getPaid() != null) {
            event.setPaid(updateRequest.getPaid());
        }
        if (updateRequest.getParticipantLimit() != null) {
            if (updateRequest.getParticipantLimit() < ValidationConstants.EVENT_PARTICIPANT_LIMIT_MIN) {
                log.warn("Некорректный лимит участников: {}", updateRequest.getParticipantLimit());
                throw new BadRequestException("Participant limit must be non-negative");
            }
            event.setParticipantLimit(updateRequest.getParticipantLimit());
        }
        if (updateRequest.getRequestModeration() != null) {
            event.setRequestModeration(updateRequest.getRequestModeration());
        }
        if (updateRequest.getLocation() != null) {
            event.setLocation(updateRequest.getLocation());
        }
        if (updateRequest.getCategory() != null) {
            var category = categoryRepository.findById(updateRequest.getCategory())
                    .orElseThrow(() -> {
                        log.warn("Категория с id={} не найдена", updateRequest.getCategory());
                        return new NotFoundException("Category with id=" + updateRequest.getCategory() + " was not found");
                    });
            event.setCategory(category);
        }

        Event updated = eventRepository.save(event);
        log.info("Событие с id={} успешно обновлено", eventId);
        return EventMapper.toDto(updated);
    }

    private User getUserOrCreate(Long userId) {
        return userRepository.findById(userId).orElseGet(() -> {
            log.info("Пользователь с id={} не найден в локальной БД, запрашиваем из user-service", userId);
            try {
                List<UserDto> users = userServiceClient.getUsers(List.of(userId));
                if (users == null || users.isEmpty()) {
                    log.error("Пользователь с id={} НЕ НАЙДЕН в user-service (пустой список)", userId);
                    throw new NotFoundException("User with id=" + userId + " was not found");
                }
                UserDto userDto = users.get(0);
                log.info("Получены данные пользователя из user-service: id={}, name={}, email={}",
                        userDto.getId(), userDto.getName(), userDto.getEmail());
                User user = User.builder()
                        .id(userDto.getId())
                        .name(userDto.getName())
                        .email(userDto.getEmail())
                        .build();
                User saved = userRepository.save(user);
                log.info("Пользователь сохранен в локальной БД с id={}", saved.getId());
                return saved;
            } catch (Exception e) {
                log.error("Ошибка получения пользователя из user-service: {}", e.getMessage(), e);
                throw new NotFoundException("User with id=" + userId + " was not found");
            }
        });
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
}