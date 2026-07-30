package ru.practicum.web.admin.service;

import ru.practicum.dto.EventDto;
import ru.practicum.dto.UpdateEventAdminRequest;

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

    void updateConfirmedRequests(Long eventId, int delta);

    boolean existsByCategoryId(Long categoryId);

    EventDto getEventById(Long eventId);
}
