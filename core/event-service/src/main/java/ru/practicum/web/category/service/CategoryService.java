package ru.practicum.web.category.service;

import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.NewCategoryDto;

import java.util.List;

public interface CategoryService {
    CategoryDto create(NewCategoryDto dto);
    CategoryDto update(Long catId, CategoryDto dto);
    void delete(Long catId);
    List<CategoryDto> getAll(int from, int size);
    CategoryDto getById(Long catId);
}
