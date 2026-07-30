package ru.practicum.statsclient;

/**
 * Исключение клиента сервиса статистики.
 * Бросается при сетевых ошибках, таймаутах или ответах сервера с кодами 4xx/5xx.
 */
public class StatsClientException extends RuntimeException {
    public StatsClientException(String message) {
        super(message);
    }

    public StatsClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
