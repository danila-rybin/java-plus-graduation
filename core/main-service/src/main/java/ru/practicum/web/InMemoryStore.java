package ru.practicum.web;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;
import ru.practicum.web.event.dto.EventDto;
import ru.practicum.web.user.dto.UserDto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Очень простой in-memory стор для нужд Postman-тестов.
 * Не потокобезопасен для серьёзной нагрузки, но этого достаточно для проверки сценариев.
 */
@Component
public class InMemoryStore {
    private final List<UserDto> users = Collections.synchronizedList(new ArrayList<>());
    private final List<EventDto> events = Collections.synchronizedList(new ArrayList<>());
    private final AtomicLong userSeq = new AtomicLong(1);
    private final AtomicLong eventSeq = new AtomicLong(1);

    @PostConstruct
    public void seed() {
        // Предзаполним несколько событий, чтобы GET /events и /events/{id} имели, что возвращать
        createEvent(EventDto.builder().title("Sample Event 1").annotation("Demo 1").build());
        createEvent(EventDto.builder().title("Sample Event 2").annotation("Demo 2").build());
    }

    public UserDto createUser(UserDto dto) {
        UserDto created = UserDto.builder()
                .id(userSeq.getAndIncrement())
                .name(dto.getName())
                .email(dto.getEmail())
                .build();
        users.add(created);
        return created;
    }

    public List<UserDto> getUsers() {
        return new ArrayList<>(users);
    }

    public EventDto createEvent(EventDto dto) {
        EventDto created = EventDto.builder()
                .id(eventSeq.getAndIncrement())
                .title(dto.getTitle())
                .annotation(dto.getAnnotation())
                .build();
        events.add(created);
        return created;
    }

    public List<EventDto> getEvents() {
        return new ArrayList<>(events);
    }

    public Optional<EventDto> getEventById(long id) {
        return events.stream().filter(e -> e.getId() == id).findFirst();
    }
}
