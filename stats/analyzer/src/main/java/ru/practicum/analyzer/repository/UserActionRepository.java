package ru.practicum.analyzer.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.analyzer.model.UserAction;

import java.util.List;

public interface UserActionRepository extends JpaRepository<UserAction, Long> {

    List<UserAction> findByUserIdOrderByTimestampDesc(Long userId);

    List<UserAction> findByEventId(Long eventId);

    @Query("SELECT ua.userId, MAX(ua.weight) FROM UserAction ua WHERE ua.eventId = :eventId GROUP BY ua.userId")
    List<Object[]> findMaxWeightsByEventId(@Param("eventId") Long eventId);

    @Query("SELECT ua FROM UserAction ua WHERE ua.userId = :userId AND ua.eventId = :eventId")
    List<UserAction> findByUserIdAndEventId(@Param("userId") Long userId, @Param("eventId") Long eventId);
}