package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "request-service")
public interface RequestServiceClient {

    @GetMapping("/internal/users/{userId}/participated")
    boolean isUserParticipated(@PathVariable("userId") Long userId,
                               @RequestParam("eventId") Long eventId);
}