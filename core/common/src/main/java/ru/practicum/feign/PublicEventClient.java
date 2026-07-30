package ru.practicum.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.EventDto;

@FeignClient(
        name = "event-service",
        contextId = "publicEventClient",
        path = "/events"
)
public interface PublicEventClient {

    @GetMapping("/{id}")
    ResponseEntity<EventDto> getEvent(@PathVariable("id") Long id);
}

