package ru.practicum.web.admin.service;

import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.UserDto;

import java.util.List;

public interface AdminUserService {

    UserDto create(NewUserRequest dto);

    void delete(Long userId);

    List<UserDto> getUsers(List<Long> ids, int from, int size);
}