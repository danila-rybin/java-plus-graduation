package ru.practicum.statsclient;

import ru.practicum.dto.EndpointHitDto;
import ru.practicum.dto.ViewStatsDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * HTTP клиент для сервиса статистики.
 */
public interface StatsClient {
    /**
     * Отправляет информацию о просмотре в сервис статистики.
     *
     * @param dto информация о просмотре
     */
    void hit(EndpointHitDto dto);

    /**
     * Запрашивает статистику за указанный период с опциональной фильтрацией по URI.
     *
     * @param start  начало интервала (включительно)
     * @param end    конец интервала (включительно или исключительно в зависимости от реализации сервера)
     * @param uris   список URI для фильтрации; может быть null или пустым для запроса всех данных
     * @param unique учитывать только уникальные IP
     * @return список статистики просмотров
     */
    List<ViewStatsDto> getStats(LocalDateTime start, LocalDateTime end, List<String> uris, boolean unique);
}
