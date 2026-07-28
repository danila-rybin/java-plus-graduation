package ru.practicum.web.admin.service;

import ru.practicum.web.admin.entity.UpdateEventAdminRequest;
import ru.practicum.web.event.dto.EventDto;

import java.util.List;

public interface AdminEventService {

    List<EventDto> getEvents(
            List<Long> users,
            List<String> states,
            List<Long> categories,
            String rangeStart,
            String rangeEnd,
            int from,
            int size
    );

    EventDto updateEvent(Long eventId, UpdateEventAdminRequest updateRequest);
}