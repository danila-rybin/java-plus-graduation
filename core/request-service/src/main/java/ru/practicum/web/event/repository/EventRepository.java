package ru.practicum.web.event.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.web.event.entity.Event;

public interface EventRepository extends JpaRepository<Event, Long> {
}
