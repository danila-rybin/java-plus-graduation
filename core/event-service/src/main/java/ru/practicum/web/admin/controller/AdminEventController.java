package ru.practicum.web.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.EventDto;
import ru.practicum.dto.UpdateEventAdminRequest;
import ru.practicum.feign.AdminEventsClient;
import ru.practicum.feign.EventClient;
import ru.practicum.web.admin.service.AdminEventService;

import java.util.List;

@RestController
@RequestMapping("/admin/events")
@RequiredArgsConstructor
@Validated
public class AdminEventController implements AdminEventsClient, EventClient {

    private final AdminEventService service;

    @GetMapping
    public ResponseEntity<List<EventDto>> getEvents(
            @RequestParam(required = false) List<Long> users,
            @RequestParam(required = false) List<String> states,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) String rangeStart,
            @RequestParam(required = false) String rangeEnd,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<EventDto> events = service.getEvents(users, states, categories, rangeStart, rangeEnd, from, size);
        return ResponseEntity.ok(events);
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventDto> updateEvent(
            @PathVariable Long eventId,
            @RequestBody @Valid UpdateEventAdminRequest updateRequest
    ) {
        EventDto updated = service.updateEvent(eventId, updateRequest);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{eventId}/confirmed-requests")
    public ResponseEntity<Void> updateConfirmedRequests(
            @PathVariable Long eventId,
            @RequestParam("delta") int delta
    ) {
        service.updateConfirmedRequests(eventId, delta);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/exists-by-category")
    public ResponseEntity<Boolean> existsByCategoryId(@RequestParam("categoryId") Long categoryId) {
        boolean exists = service.existsByCategoryId(categoryId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDto> findById(@PathVariable Long eventId) {
        EventDto event = service.getEventById(eventId);
        return ResponseEntity.ok(event);
    }
}
