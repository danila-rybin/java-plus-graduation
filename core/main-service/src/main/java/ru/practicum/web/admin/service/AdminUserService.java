package ru.practicum.web.admin.service;

import ru.practicum.web.user.dto.NewUserRequest;
import ru.practicum.web.user.dto.UserDto;

import java.util.List;

public interface AdminUserService {

    UserDto create(NewUserRequest dto);

    void delete(Long userId);

    List<UserDto> getUsers(List<Long> ids, int from, int size);
}