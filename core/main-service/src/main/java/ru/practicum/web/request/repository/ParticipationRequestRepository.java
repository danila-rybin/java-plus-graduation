package ru.practicum.web.request.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.web.request.entity.ParticipationRequest;
import ru.practicum.web.request.entity.RequestStatus;

import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    List<ParticipationRequest> findAllByRequesterId(Long userId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    List<ParticipationRequest> findAllByEventInitiatorId(Long userId);

    Optional<ParticipationRequest> findByEventIdAndRequesterId(Long eventId, Long requesterId);

    boolean existsByEventIdAndRequesterId(Long eventId, Long requesterId);

    int countByEventIdAndStatus(Long eventId, RequestStatus status);
}