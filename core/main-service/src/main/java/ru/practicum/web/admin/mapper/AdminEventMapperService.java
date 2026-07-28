package ru.practicum.web.admin.mapper;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.practicum.web.admin.entity.UpdateEventAdminRequest;
import ru.practicum.web.event.entity.Event;
import ru.practicum.web.event.entity.EventStatus;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AdminEventMapperService {

    public void updateEventFields(Event event, UpdateEventAdminRequest request,
                                  LocalDateTime eventDate, ru.practicum.web.admin.entity.Category category) {

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }
        if (request.getAnnotation() != null) {
            event.setAnnotation(request.getAnnotation());
        }
        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }
        if (eventDate != null) {
            event.setEventDate(eventDate);
        }
        if (request.getPaid() != null) {
            event.setPaid(request.getPaid());
        }
        if (request.getParticipantLimit() != null) {
            event.setParticipantLimit(request.getParticipantLimit());
        }
        if (request.getRequestModeration() != null) {
            event.setRequestModeration(request.getRequestModeration());
        }
        if (category != null) {
            event.setCategory(category);
        }
    }

    public void applyStateAction(Event event, String stateAction) {
        if (stateAction == null) return;

        switch (stateAction) {
            case "PUBLISH_EVENT":
                event.setStatus(EventStatus.PUBLISHED);
                event.setPublishedOn(LocalDateTime.now());
                break;
            case "REJECT_EVENT":
                event.setStatus(EventStatus.CANCELED);
                break;
            default:
                throw new IllegalArgumentException("Invalid state action: " + stateAction);
        }
    }
}