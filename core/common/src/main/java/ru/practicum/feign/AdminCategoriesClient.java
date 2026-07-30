package ru.practicum.feign;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.NewCategoryDto;
import ru.practicum.validation.ValidationConstants;

import java.util.List;

@FeignClient(
        name = "event-service",
        contextId = "adminCategoriesClient",
        path = "/admin/categories"
)
public interface AdminCategoriesClient {

    @PostMapping
    ResponseEntity<CategoryDto> create(@Valid @RequestBody NewCategoryDto dto);

    @PatchMapping("/{catId}")
    ResponseEntity<CategoryDto> update(
            @PathVariable("catId") Long catId,
            @Valid @RequestBody CategoryDto dto
    );

    @DeleteMapping("/{catId}")
    ResponseEntity<Void> delete(@PathVariable("catId") Long catId);

    @GetMapping
    ResponseEntity<List<CategoryDto>> getAll(
            @RequestParam(defaultValue = ValidationConstants.PAGE_DEFAULT_FROM + "") int from,
            @RequestParam(defaultValue = ValidationConstants.PAGE_DEFAULT_SIZE + "") int size
    );

    @GetMapping("/{catId}")
    ResponseEntity<CategoryDto> getById(@PathVariable("catId") Long catId);
}
