package ru.practicum.web.admin.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.web.admin.repository.CategoryRepository;
import ru.practicum.web.event.repository.EventRepository;
import ru.practicum.web.exception.BadRequestException;
import ru.practicum.web.exception.ConflictException;
import ru.practicum.web.exception.NotFoundException;
import ru.practicum.web.validation.ValidationConstants;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryValidator {

    private final CategoryRepository categoryRepository;
    private final EventRepository eventRepository;

    public void validateCategoryName(String name) {
        log.debug("Валидация названия категории: {}", name);

        if (name == null || name.isBlank()) {
            log.warn("Название категории не может быть пустым");
            throw new BadRequestException("Category name cannot be empty");
        }
        if (name.length() > ValidationConstants.CATEGORY_NAME_MAX) {
            log.warn("Некорректная длина названия категории: {}, максимум {}",
                    name.length(), ValidationConstants.CATEGORY_NAME_MAX);
            throw new BadRequestException("Category name length must be between 1 and " +
                    ValidationConstants.CATEGORY_NAME_MAX + " characters");
        }
    }

    public void validateCategoryExists(Long id) {
        log.debug("Проверка существования категории с id={}", id);

        if (!categoryRepository.existsById(id)) {
            log.warn("Категория с id={} не найдена", id);
            throw new NotFoundException("Category with id=" + id + " was not found");
        }
    }

    public void checkCategoryNameUnique(String name) {
        log.debug("Проверка уникальности названия категории: {}", name);

        if (categoryRepository.existsByName(name)) {
            log.warn("Категория с названием '{}' уже существует", name);
            throwConflictException();
        }
    }

    public void checkCategoryNameUniqueForUpdate(String name, Long id) {
        log.debug("Проверка уникальности названия категории при обновлении: name={}, id={}", name, id);

        if (categoryRepository.existsByNameAndIdNot(name, id)) {
            log.warn("Категория с названием '{}' уже существует (кроме обновляемой с id={})", name, id);
            throwConflictException();
        }
    }

    public void checkCategoryNotInUse(Long categoryId) {
        log.debug("Проверка использования категории с id={} в событиях", categoryId);

        if (eventRepository.existsByCategoryId(categoryId)) {
            log.warn("Категория с id={} используется в событиях и не может быть удалена", categoryId);
            throw new ConflictException("The category is not empty");
        }
    }

    public void validatePagination(int from, int size) {
        log.debug("Валидация параметров пагинации: from={}, size={}", from, size);

        if (from < ValidationConstants.PAGE_MIN_FROM) {
            log.warn("Некорректное значение from: {}", from);
            throw new BadRequestException("Parameter 'from' must be non-negative");
        }
        if (size < ValidationConstants.PAGE_MIN_SIZE) {
            log.warn("Некорректное значение size: {}", size);
            throw new BadRequestException("Parameter 'size' must be positive");
        }
    }

    private void throwConflictException() {
        throw new ConflictException(
                "could not execute statement; SQL [n/a]; constraint [uq_category_name]; " +
                        "nested exception is org.hibernate.exception.ConstraintViolationException: could not execute statement"
        );
    }
}