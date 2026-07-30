package ru.practicum.web.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.web.exception.ConflictException;
import ru.practicum.web.user.dto.NewUserRequest;
import ru.practicum.web.user.dto.UserDto;
import ru.practicum.web.user.entity.User;
import ru.practicum.web.user.mapper.UserMapper;
import ru.practicum.web.user.repository.UserRepository;
import ru.practicum.web.user.validator.UserValidator;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final UserValidator validator;

    @Override
    public UserDto create(NewUserRequest request) {
        log.info("Создание нового пользователя: email={}, имя='{}'", request.getEmail(), request.getName());

        validator.validateCreateRequest(request);

        try {
            User user = UserMapper.fromNewUserRequest(request);
            User saved = userRepository.save(user);
            log.info("Пользователь создан с id={}, email={}, имя='{}'",
                    saved.getId(), saved.getEmail(), saved.getName());
            return UserMapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Ошибка уникальности при создании пользователя с email: {}", request.getEmail());
            validator.checkEmailUnique(request.getEmail());
            throw new ConflictException("User with email " + request.getEmail() + " already exists");
        }
    }

    @Override
    public void delete(Long userId) {
        log.info("Удаление пользователя с id={}", userId);

        validator.validateUserExists(userId);
        userRepository.deleteById(userId);
        log.info("Пользователь с id={} удален", userId);
    }

    @Override
    public List<UserDto> getUsers(List<Long> ids, int from, int size) {
        log.debug("Запрос списка пользователей: ids={}, from={}, size={}", ids, from, size);

        validator.validatePagination(from, size);

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        List<UserDto> users;
        if (ids != null && !ids.isEmpty()) {
            users = userRepository.findByIdIn(ids, pageable)
                    .stream()
                    .map(UserMapper::toDto)
                    .collect(Collectors.toList());
            log.debug("Запрошены пользователи с конкретными id: {}", ids);
        } else {
            users = userRepository.findAll(pageable)
                    .stream()
                    .map(UserMapper::toDto)
                    .collect(Collectors.toList());
        }

        log.debug("Найдено {} пользователей", users.size());
        return users;
    }
}