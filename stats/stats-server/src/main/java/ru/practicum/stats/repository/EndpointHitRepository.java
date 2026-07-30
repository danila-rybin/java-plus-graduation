package ru.practicum.stats.repository;

import ru.practicum.stats.model.EndpointHit;
import ru.practicum.dto.ViewStatsDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EndpointHitRepository extends JpaRepository<EndpointHit, Long> {

    // Стандартная агрегированная статистика по всем URI
    @Query("""
        SELECT new ru.practicum.dto.ViewStatsDto(
            h.app,
            h.uri,
            COUNT(h.id)
        )
        FROM EndpointHit h
        WHERE h.timestamp BETWEEN :start AND :end
        GROUP BY h.app, h.uri
        ORDER BY COUNT(h.id) DESC
    """)
    List<ViewStatsDto> findStats(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Статистика только по переданным URI (не-уникальные хиты)
    @Query("""
        SELECT new ru.practicum.dto.ViewStatsDto(
            h.app,
            h.uri,
            COUNT(h.id)
        )
        FROM EndpointHit h
        WHERE h.timestamp BETWEEN :start AND :end
          AND h.uri IN :uris
        GROUP BY h.app, h.uri
        ORDER BY COUNT(h.id) DESC
    """)
    List<ViewStatsDto> findStatsByUris(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris
    );

    // Уникальные посещения по всем URI
    @Query("""
        SELECT new ru.practicum.dto.ViewStatsDto(
            h.app,
            h.uri,
            COUNT(DISTINCT h.ip)
        )
        FROM EndpointHit h
        WHERE h.timestamp BETWEEN :start AND :end
        GROUP BY h.app, h.uri
        ORDER BY COUNT(DISTINCT h.ip) DESC
    """)
    List<ViewStatsDto> findUniqueStats(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );

    // Уникальные посещения только по переданным URI
    @Query("""
        SELECT new ru.practicum.dto.ViewStatsDto(
            h.app,
            h.uri,
            COUNT(DISTINCT h.ip)
        )
        FROM EndpointHit h
        WHERE h.timestamp BETWEEN :start AND :end
          AND h.uri IN :uris
        GROUP BY h.app, h.uri
        ORDER BY COUNT(DISTINCT h.ip) DESC
    """)
    List<ViewStatsDto> findUniqueStatsByUris(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("uris") List<String> uris
    );
}