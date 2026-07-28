package ru.practicum.web.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.event.dto.EventShortDto;
import ru.practicum.web.event.dto.NewEventDto;
import ru.practicum.web.event.dto.UpdateEventUserRequest;
import ru.practicum.web.event.service.PrivateEventService;
import ru.practicum.web.exception.BadRequestException;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/events")
@RequiredArgsConstructor
@Validated
public class PrivateEventController {

    private final PrivateEventService privateEventService;

    @GetMapping
    public ResponseEntity<List<EventShortDto>> getEvents(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (from < 0) {
            throw new BadRequestException("Parameter 'from' must be non-negative");
        }
        if (size <= 0) {
            throw new BadRequestException("Parameter 'size' must be positive");
        }
        List<EventShortDto> events = privateEventService.getEvents(userId, from, size);
        return ResponseEntity.ok(events);
    }

    @PostMapping
    public ResponseEntity<EventDto> addEvent(
            @PathVariable Long userId,
            @RequestBody @Valid NewEventDto dto
    ) {
        EventDto created = privateEventService.addEvent(userId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<EventDto> getEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId
    ) {
        EventDto event = privateEventService.getEvent(userId, eventId);
        return ResponseEntity.ok(event);
    }

    @PatchMapping("/{eventId}")
    public ResponseEntity<EventDto> updateEvent(
            @PathVariable Long userId,
            @PathVariable Long eventId,
            @RequestBody @Valid UpdateEventUserRequest updateRequest
    ) {
        EventDto updated = privateEventService.updateEvent(userId, eventId, updateRequest);
        return ResponseEntity.ok(updated);
    }
}