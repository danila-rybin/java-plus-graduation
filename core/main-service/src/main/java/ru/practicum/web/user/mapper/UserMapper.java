package ru.practicum.web.user.mapper;

import lombok.experimental.UtilityClass;
import ru.practicum.web.user.dto.UserDto;
import ru.practicum.web.user.entity.User;

@UtilityClass
public class UserMapper {

    public User toEntity(UserDto dto) {
        return User.builder()
                .name(dto.getName())
                .email(dto.getEmail())
                .build();
    }

    public UserDto toDto(User user) {
        return new UserDto(
                user.getId(),
                user.getName(),
                user.getEmail()
        );
    }

    public static User fromNewUserRequest(ru.practicum.web.user.dto.NewUserRequest request) {
        if (request == null) {
            return null;
        }
        return User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
    }
}