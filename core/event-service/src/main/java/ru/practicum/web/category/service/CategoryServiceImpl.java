package ru.practicum.web.category.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.NewCategoryDto;
import ru.practicum.web.category.entity.Category;
import ru.practicum.web.category.mapper.CategoryMapper;
import ru.practicum.web.category.repository.CategoryRepository;
import ru.practicum.web.category.validation.CategoryValidator;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryValidator validator;

    public CategoryServiceImpl(
            CategoryRepository categoryRepository,
            @Qualifier("eventServiceCategoryValidator") CategoryValidator validator
    ) {
        this.categoryRepository = categoryRepository;
        this.validator = validator;
    }

    @Override
    public CategoryDto create(NewCategoryDto dto) {
        log.info("Создание новой категории: '{}'", dto.getName());

        validator.validateCategoryName(dto.getName());
        validator.checkCategoryNameUnique(dto.getName());

        Category category = Category.builder()
                .name(dto.getName())
                .build();

        Category saved = categoryRepository.save(category);
        log.info("Категория создана с id={}, name={}", saved.getId(), saved.getName());
        return CategoryMapper.toDto(saved);
    }

    @Override
    public CategoryDto update(Long catId, CategoryDto dto) {
        log.info("Обновление категории с id={}", catId);

        validator.validateCategoryExists(catId);
        validator.validateCategoryName(dto.getName());
        validator.checkCategoryNameUniqueForUpdate(dto.getName(), catId);

        Category category = categoryRepository.findById(catId).orElseThrow();
        category.setName(dto.getName());

        Category updated = categoryRepository.save(category);
        log.info("Категория с id={} обновлена", catId);
        return CategoryMapper.toDto(updated);
    }

    @Override
    public void delete(Long catId) {
        log.info("Удаление категории с id={}", catId);

        validator.validateCategoryExists(catId);
        validator.checkCategoryNotInUse(catId);

        categoryRepository.deleteById(catId);
        log.info("Категория с id={} удалена", catId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAll(int from, int size) {
        log.debug("Получение списка категорий: from={}, size={}", from, size);

        validator.validatePagination(from, size);

        Pageable pageable = PageRequest.of(from / size, size);
        return categoryRepository.findAll(pageable).getContent()
                .stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto getById(Long catId) {
        log.debug("Получение категории с id={}", catId);

        validator.validateCategoryExists(catId);

        Category category = categoryRepository.findById(catId).orElseThrow();
        return CategoryMapper.toDto(category);
    }
}
