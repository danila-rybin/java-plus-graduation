package ru.practicum.web.request.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.web.request.service.PrivateRequestService;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalRequestController {

    private final PrivateRequestService requestService;

    @GetMapping("/{userId}/participated")
    public boolean isUserParticipated(@PathVariable Long userId,
                                      @RequestParam Long eventId) {
        return requestService.isUserParticipated(userId, eventId);
    }
}