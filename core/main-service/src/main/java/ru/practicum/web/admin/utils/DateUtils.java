package ru.practicum.web.admin.utils;

import org.springframework.stereotype.Component;
import ru.practicum.web.exception.BadRequestException;
import ru.practicum.web.validation.ValidationConstants;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Component
public class DateUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern(ValidationConstants.DATE_TIME_FORMAT);

    public LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr, FORMATTER);
        } catch (DateTimeParseException e) {
            try {
                return LocalDateTime.parse(dateTimeStr);
            } catch (DateTimeParseException e2) {
                throw new BadRequestException("Invalid date format. Expected: " +
                        ValidationConstants.DATE_TIME_FORMAT + " or ISO format");
            }
        }
    }

    public LocalDateTime parseEventDate(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isBlank()) {
            return null;
        }
        return parseDateTime(dateTimeStr);
    }

    public boolean isBeforeNowPlusHours(LocalDateTime dateTime, long hours) {
        return dateTime.isBefore(LocalDateTime.now().plusHours(hours));
    }
}