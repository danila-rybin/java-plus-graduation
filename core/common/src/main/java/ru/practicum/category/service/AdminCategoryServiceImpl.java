package ru.practicum.category.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.category.entity.Category;
import ru.practicum.category.mapper.CategoryMapper;
import ru.practicum.category.repository.CategoryRepository;
import ru.practicum.dto.CategoryDto;
import ru.practicum.dto.NewCategoryDto;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.feign.EventClient;
import ru.practicum.validation.CategoryValidator;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AdminCategoryServiceImpl implements AdminCategoryService {

    private final CategoryRepository categoryRepository;
    private final EventClient eventClient;
    private final CategoryValidator validator;

    @Override
    public CategoryDto create(NewCategoryDto dto) {
        log.info("Создание новой категории: {}", dto.getName());

        validator.validateCategoryName(dto.getName());
        validator.checkCategoryNameUnique(dto.getName());

        try {
            Category category = Category.builder()
                    .name(dto.getName())
                    .build();
            Category saved = categoryRepository.save(category);
            log.info("Категория создана с id={}", saved.getId());
            return CategoryMapper.toDto(saved);
        } catch (DataIntegrityViolationException e) {
            log.warn("Ошибка уникальности при создании категории: {}", dto.getName());
            validator.checkCategoryNameUnique(dto.getName());
            throw e;
        }
    }

    @Override
    public CategoryDto update(Long id, CategoryDto dto) {
        log.info("Обновление категории с id={}: новое название '{}'", id, dto.getName());

        validator.validateCategoryName(dto.getName());
        validator.validateCategoryExists(id);
        validator.checkCategoryNameUniqueForUpdate(dto.getName(), id);

        Category category = getCategoryOrThrow(id);
        String oldName = category.getName();
        category.setName(dto.getName());

        Category updated = categoryRepository.save(category);
        log.info("Категория с id={} обновлена: '{}' -> '{}'", id, oldName, dto.getName());
        return CategoryMapper.toDto(updated);
    }

    @Override
    public void delete(Long id) {
        log.info("Удаление категории с id={}", id);

        validator.validateCategoryExists(id);
        checkCategoryNotInUse(id);

        categoryRepository.deleteById(id);
        log.info("Категория с id={} удалена", id);
    }

    @Override
    public List<CategoryDto> getAll(int from, int size) {
        log.debug("Запрос списка категорий: from={}, size={}", from, size);

        validator.validatePagination(from, size);

        int page = from / size;
        Pageable pageable = PageRequest.of(page, size);

        List<CategoryDto> categories = categoryRepository.findAll(pageable)
                .stream()
                .map(CategoryMapper::toDto)
                .collect(Collectors.toList());

        log.debug("Найдено {} категорий", categories.size());
        return categories;
    }

    @Override
    public CategoryDto getById(Long id) {
        log.debug("Запрос категории с id={}", id);
        return CategoryMapper.toDto(getCategoryOrThrow(id));
    }

    private Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Категория с id={} не найдена", id);
                    return new NotFoundException("Category with id=" + id + " was not found");
                });
    }

    private void checkCategoryNotInUse(Long categoryId) {
        Boolean exists = eventClient.existsByCategoryId(categoryId).getBody();
        if (Boolean.TRUE.equals(exists)) {
            log.warn("Попытка удалить категорию с id={}, которая используется в событиях", categoryId);
            throw new ConflictException("The category is not empty");
        }
    }
}