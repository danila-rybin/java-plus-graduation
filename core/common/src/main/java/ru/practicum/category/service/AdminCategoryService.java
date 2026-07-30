package ru.practicum.category.service;


import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.NewCategoryDto;

import java.util.List;

public interface AdminCategoryService {

    CategoryDto create(NewCategoryDto dto);

    CategoryDto update(Long id, CategoryDto dto);

    void delete(Long id);

    List<CategoryDto> getAll(int from, int size);

    CategoryDto getById(Long id);
}