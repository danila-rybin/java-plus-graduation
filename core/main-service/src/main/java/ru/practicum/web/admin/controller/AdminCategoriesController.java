package ru.practicum.web.admin.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.web.admin.dto.CategoryDto;
import ru.practicum.web.admin.dto.NewCategoryDto;
import ru.practicum.web.admin.service.AdminCategoryService;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
@Validated
public class AdminCategoriesController {

    private final AdminCategoryService service;

    @PostMapping
    public ResponseEntity<CategoryDto> create(@RequestBody @Valid NewCategoryDto dto) {
        CategoryDto created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{catId}")
    public ResponseEntity<CategoryDto> update(
            @PathVariable Long catId,
            @RequestBody @Valid NewCategoryDto dto
    ) {
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setName(dto.getName());
        CategoryDto updated = service.update(catId, categoryDto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{catId}")
    public ResponseEntity<Void> delete(@PathVariable Long catId) {
        service.delete(catId);
        return ResponseEntity.noContent().build();
    }
}