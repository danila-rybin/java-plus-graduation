package ru.practicum.web.category.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.CategoryDto;
import ru.practicum.web.category.service.CategoryService;

import java.util.List;

@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
@Validated
public class PublicCategoriesController {

    private final CategoryService categoryService;

    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories(
            @RequestParam(defaultValue = "0") int from,
            @RequestParam(defaultValue = "10") int size
    ) {
        List<CategoryDto> categories = categoryService.getAll(from, size);
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/{catId}")
    public ResponseEntity<CategoryDto> getCategory(@PathVariable Long catId) {
        CategoryDto category = categoryService.getById(catId);
        return ResponseEntity.ok(category);
    }
}
