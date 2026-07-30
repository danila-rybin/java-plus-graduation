package ru.practicum.feign;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.NewUserRequest;
import ru.practicum.dto.UserDto;
import ru.practicum.validation.ValidationConstants;

import java.util.List;

@FeignClient(
        name = "user-service",
        contextId = "adminUsersClient",
        path = "/admin/users"
)
public interface AdminUsersClient {

    @PostMapping
    ResponseEntity<UserDto> create(@Valid @RequestBody NewUserRequest request);

    @GetMapping
    ResponseEntity<List<UserDto>> getUsers(
            @RequestParam(required = false) List<Long> ids,
            @RequestParam(defaultValue = ValidationConstants.PAGE_DEFAULT_FROM + "") int from,
            @RequestParam(defaultValue = ValidationConstants.PAGE_DEFAULT_SIZE + "") int size
    );

    @DeleteMapping("/{userId}")
    ResponseEntity<Void> delete(@PathVariable("userId") Long userId);
}