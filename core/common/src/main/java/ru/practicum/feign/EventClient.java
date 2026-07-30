package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.EventDto;

@FeignClient(
        name = "event-service",
        contextId = "eventClient",
        path = "/admin/events"
)
public interface EventClient {

    @GetMapping("/exists-by-category")
    ResponseEntity<Boolean> existsByCategoryId(@RequestParam("categoryId") Long categoryId);

    @GetMapping("/{eventId}")
    ResponseEntity<EventDto> findById(@PathVariable("eventId") Long eventId);
}
