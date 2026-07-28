package ru.practicum.web.user.validator;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.web.exception.BadRequestException;
import ru.practicum.web.exception.ConflictException;
import ru.practicum.web.exception.NotFoundException;
import ru.practicum.web.user.dto.NewUserRequest;
import ru.practicum.web.user.repository.UserRepository;
import ru.practicum.web.validation.ValidationConstants;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserValidator {

    private final UserRepository userRepository;

    public void validateCreateRequest(NewUserRequest request) {
        log.debug("Валидация данных нового пользователя");
        validateEmail(request.getEmail());
        validateName(request.getName());
        checkEmailUnique(request.getEmail());
    }

    public void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            log.warn("Email не может быть пустым");
            throw new BadRequestException("Email must not be blank");
        }
        if (email.length() < ValidationConstants.USER_EMAIL_MIN ||
                email.length() > ValidationConstants.USER_EMAIL_MAX) {
            log.warn("Некорректная длина email: {}", email.length());
            throw new BadRequestException("Email length must be between " +
                    ValidationConstants.USER_EMAIL_MIN + " and " + ValidationConstants.USER_EMAIL_MAX + " characters");
        }
        if (!email.contains("@")) {
            log.warn("Некорректный формат email: {}", email);
            throw new BadRequestException("Invalid email format");
        }
    }

    public void validateName(String name) {
        if (name == null || name.isBlank()) {
            log.warn("Имя не может быть пустым");
            throw new BadRequestException("Name must not be blank");
        }
        if (name.length() < ValidationConstants.USER_NAME_MIN ||
                name.length() > ValidationConstants.USER_NAME_MAX) {
            log.warn("Некорректная длина имени: {}", name.length());
            throw new BadRequestException("Name length must be between " +
                    ValidationConstants.USER_NAME_MIN + " and " + ValidationConstants.USER_NAME_MAX + " characters");
        }
    }

    public void checkEmailUnique(String email) {
        if (userRepository.existsByEmail(email)) {
            log.warn("Email {} уже занят", email);
            throw new ConflictException("User with email " + email + " already exists");
        }
    }

    public void validateUserExists(Long userId) {
        if (!userRepository.existsById(userId)) {
            log.warn("Пользователь с id={} не найден", userId);
            throw new NotFoundException("User with id=" + userId + " was not found");
        }
    }

    public void validatePagination(int from, int size) {
        if (from < ValidationConstants.PAGE_MIN_FROM) {
            log.warn("Некорректное значение from: {}", from);
            throw new BadRequestException("Parameter 'from' must be non-negative");
        }
        if (size < ValidationConstants.PAGE_MIN_SIZE) {
            log.warn("Некорректное значение size: {}", size);
            throw new BadRequestException("Parameter 'size' must be positive");
        }
    }
}