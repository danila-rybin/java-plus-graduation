package ru.practicum.web.event.service;

import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.dto.EventShortDto;
import ru.practicum.web.event.dto.NewEventDto;
import ru.practicum.web.event.dto.UpdateEventUserRequest;

import java.util.List;

public interface PrivateEventService {

    List<EventShortDto> getEvents(Long userId, int from, int size);

    EventDto addEvent(Long userId, NewEventDto dto);

    EventDto getEvent(Long userId, Long eventId);

    EventDto updateEvent(Long userId, Long eventId, UpdateEventUserRequest updateRequest);
}