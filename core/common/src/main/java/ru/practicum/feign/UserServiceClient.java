package ru.practicum.feign;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.practicum.dto.UserDto;

import java.util.List;

@FeignClient(name = "user-service")
public interface UserServiceClient {

    Logger log = LoggerFactory.getLogger(UserServiceClient.class);

    @GetMapping("/admin/users")
    List<UserDto> getUsers(@RequestParam("ids") List<Long> ids);

    default boolean userExists(Long userId) {
        try {
            List<UserDto> users = getUsers(List.of(userId));
            return users != null && !users.isEmpty();
        } catch (Exception e) {
            log.warn("Ошибка проверки пользователя в user-service: id={}, error={}", userId, e.getMessage());
            return false;
        }
    }
}