package ru.practicum.web.admin.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.web.admin.dto.NewCompilationDto;
import ru.practicum.web.admin.entity.UpdateCompilationRequest;
import ru.practicum.web.exception.BadRequestException;
import ru.practicum.web.exception.NotFoundException;
import ru.practicum.web.validation.ValidationConstants;

@Slf4j
@Component
@RequiredArgsConstructor
public class CompilationValidator {

    public void validateTitle(String title) {
        log.debug("Валидация заголовка подборки: {}", title);

        if (title == null || title.isBlank()) {
            log.warn("Заголовок подборки не может быть пустым");
            throw new BadRequestException("Title must not be blank");
        }
        if (title.length() < ValidationConstants.COMPILATION_TITLE_MIN ||
                title.length() > ValidationConstants.COMPILATION_TITLE_MAX) {
            log.warn("Некорректная длина заголовка: {}, допустимо от {} до {}",
                    title.length(), ValidationConstants.COMPILATION_TITLE_MIN,
                    ValidationConstants.COMPILATION_TITLE_MAX);
            throw new BadRequestException("Title length must be between " +
                    ValidationConstants.COMPILATION_TITLE_MIN + " and " +
                    ValidationConstants.COMPILATION_TITLE_MAX + " characters");
        }
    }

    public void validateTitleForUpdate(String title) {
        if (title != null) {
            log.debug("Валидация заголовка подборки при обновлении: {}", title);

            if (title.isBlank()) {
                log.warn("Заголовок подборки не может быть пустым");
                throw new BadRequestException("Title must not be blank");
            }
            if (title.length() < ValidationConstants.COMPILATION_TITLE_MIN ||
                    title.length() > ValidationConstants.COMPILATION_TITLE_MAX) {
                log.warn("Некорректная длина заголовка: {}, допустимо от {} до {}",
                        title.length(), ValidationConstants.COMPILATION_TITLE_MIN,
                        ValidationConstants.COMPILATION_TITLE_MAX);
                throw new BadRequestException("Title length must be between " +
                        ValidationConstants.COMPILATION_TITLE_MIN + " and " +
                        ValidationConstants.COMPILATION_TITLE_MAX + " characters");
            }
        }
    }

    public void validateCompilationExists(Boolean exists, Long id) {
        log.debug("Проверка существования подборки с id={}", id);

        if (!exists) {
            log.warn("Подборка с id={} не найдена", id);
            throw new NotFoundException("Compilation with id=" + id + " was not found");
        }
    }

    public void validateCreateRequest(NewCompilationDto dto) {
        log.debug("Валидация запроса на создание подборки: {}", dto.getTitle());
        validateTitle(dto.getTitle());
        log.debug("Валидация запроса на создание подборки успешно пройдена");
    }

    public void validateUpdateRequest(UpdateCompilationRequest dto) {
        log.debug("Валидация запроса на обновление подборки");
        validateTitleForUpdate(dto.getTitle());
        log.debug("Валидация запроса на обновление подборки успешно пройдена");
    }
}