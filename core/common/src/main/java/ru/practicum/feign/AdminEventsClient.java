package ru.practicum.feign;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EventDto;
import ru.practicum.dto.UpdateEventAdminRequest;

import java.util.List;

@FeignClient(
        name = "event-service",
        contextId = "adminEventsClient",
        path = "/admin/events"
)
public interface AdminEventsClient {

    @GetMapping
    ResponseEntity<List<EventDto>> getEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    );

    @RequestMapping(method = RequestMethod.PATCH, value = "/{eventId}")
    ResponseEntity<EventDto> updateEvent(
            @PathVariable("eventId") Long eventId,
            @Valid @RequestBody UpdateEventAdminRequest updateRequest
    );

    @PostMapping("/{eventId}/confirmed-requests")
    ResponseEntity<Void> updateConfirmedRequests(
            @PathVariable("eventId") Long eventId,
            @RequestParam("delta") int delta
    );
}