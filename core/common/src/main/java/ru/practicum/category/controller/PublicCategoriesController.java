package ru.practicum.category.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.category.service.AdminCategoryService;
import ru.practicum.dto.CategoryDto;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
public class PublicCategoriesController {

    private final AdminCategoryService service;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        if (size <= 0 || from < 0) {
            return ResponseEntity.badRequest().build();
        }

        List<CategoryDto> categories = service.getAll(from, size);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{catId}")
    public ResponseEntity<CategoryDto> getCategory(@PathVariable Long catId) {
        try {
            CategoryDto category = service.getById(catId);
            return ResponseEntity.ok(category);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}